package com.hutech.demo.service;

import com.hutech.demo.model.*;
import com.hutech.demo.repository.CartItemRepository;
import com.hutech.demo.repository.CartRepository;
import com.hutech.demo.repository.OrderRepository;
import com.hutech.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    /** Phí giao hàng mặc định (30.000₫) */
    public static final int SHIPPING_FEE_VND = 30_000;
    /** Ngưỡng miễn phí ship: tạm tính >= 1.000.000₫ */
    public static final BigDecimal FREE_SHIPPING_MIN_SUBTOTAL = new BigDecimal("1000000");
    /** Ngưỡng miễn phí ship: tổng số lượng >= 2 */
    public static final int FREE_SHIPPING_MIN_TOTAL_QUANTITY = 2;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final com.hutech.demo.repository.UserRepository userRepository;

    /**
     * Tính phí giao hàng dựa trên giỏ hàng:
     * - Miễn phí nếu tạm tính >= 1.000.000₫ và tổng số lượng >= 2.
     * - Ngược lại: 30.000₫.
     */
    public static int computeShippingFee(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return SHIPPING_FEE_VND;
        }
        cart.calculateTotal();
        int totalQty = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal subtotal = cart.getTotalAmount();
        if (subtotal != null
                && subtotal.compareTo(FREE_SHIPPING_MIN_SUBTOTAL) >= 0
                && totalQty >= FREE_SHIPPING_MIN_TOTAL_QUANTITY) {
            return 0;
        }
        return SHIPPING_FEE_VND;
    }

    public Order createOrderFromCart(
            User user,
            String shippingAddress,
            String phoneNumber,
            String paymentMethod,
            String note,
            int ignoredClientShippingFee
    ) {
        Cart cart = cartService.getActiveCart(user);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Tính lại tạm tính dựa trên giá gốc, % giảm giá và quota khuyến mãi còn lại
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        // Check stock trước
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }
        }

        List<OrderDetail> orderDetails = new ArrayList<>();

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setPhoneNumber(phoneNumber);
        order.setPaymentMethod(paymentMethod);
        order.setNote(note);

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            int quantity = item.getQuantity();
            totalQuantity += quantity;

            int discountPercent = product.getDiscountPercent() != null ? product.getDiscountPercent() : 0;
            BigDecimal basePrice = BigDecimal.valueOf(product.getPrice());

            // Xác định số lượng được khuyến mãi theo promoRemaining
            int promoRemain = product.getPromoRemaining() != null ? product.getPromoRemaining() : 0;
            int discountedQty = (discountPercent > 0 && promoRemain > 0)
                    ? Math.min(quantity, promoRemain)
                    : 0;
            int normalQty = quantity - discountedQty;

            BigDecimal discountedPrice = basePrice
                    .multiply(BigDecimal.valueOf(100 - discountPercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal lineTotal = discountedPrice.multiply(BigDecimal.valueOf(discountedQty))
                    .add(basePrice.multiply(BigDecimal.valueOf(normalQty)));

            subtotal = subtotal.add(lineTotal);

            // Trừ tồn kho tổng
            product.setStock(product.getStock() - quantity);
            // Trừ số lượng khuyến mãi còn lại
            if (discountedQty > 0 && product.getPromoRemaining() != null) {
                int newRemain = Math.max(0, product.getPromoRemaining() - discountedQty);
                product.setPromoRemaining(newRemain);
            }
            productRepository.save(product);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            // Lưu đơn giá trung bình để tổng tiền vẫn khớp
            BigDecimal avgUnitPrice = lineTotal
                    .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
            detail.setPrice(avgUnitPrice.doubleValue());
            orderDetails.add(detail);
        }

        // Tính phí ship dựa trên subtotal mới & tổng số lượng
        int shippingFee;
        if (subtotal.compareTo(FREE_SHIPPING_MIN_SUBTOTAL) >= 0
                && totalQuantity >= FREE_SHIPPING_MIN_TOTAL_QUANTITY) {
            shippingFee = 0;
        } else {
            shippingFee = SHIPPING_FEE_VND;
        }
        BigDecimal orderTotal = subtotal.add(BigDecimal.valueOf(shippingFee));
        order.setTotalAmount(orderTotal.doubleValue());

        // Điểm thưởng: mỗi 10.000₫ tiền hàng = 1 điểm, không tính phí ship
        int rewardPoints = subtotal
                .divide(new BigDecimal("10000"), 0, RoundingMode.DOWN)
                .intValue();
        order.setRewardPoints(rewardPoints);

        order.setOrderDetails(orderDetails);

        // Cộng dồn điểm thưởng vào User.totalRewardPoints
        if (user != null) {
            Integer current = user.getTotalRewardPoints();
            if (current == null) current = 0;
            user.setTotalRewardPoints(current + rewardPoints);
            userRepository.save(user);
        }

        Order savedOrder = orderRepository.save(order);

        // Mark cart as checked out and clear items
        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.getItems().forEach(cartItemRepository::delete);
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setUpdatedDate(LocalDateTime.now());
        cartRepository.save(cart);

        return savedOrder;
    }

    public List<Order> getOrdersForUser(User user) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
    }

    public Order getOrderForUser(Long orderId, User user) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Cancel an order for the given user if it is still cancellable.
     * Rules:
     * - Must belong to the user
     * - Must not be already CANCELLED or SHIPPED
     * - Must not be PAID
     * - Must be within 30 minutes from orderDate
     */
    public Order cancelOrderForUser(Long orderId, User user) {
        Order order = getOrderForUser(orderId, user);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled.");
        }
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new RuntimeException("Order has already been shipped and cannot be cancelled.");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Paid orders cannot be cancelled. Please contact support.");
        }
        if (order.getOrderDate() != null &&
                order.getOrderDate().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new RuntimeException("Order can only be cancelled within 30 minutes after placing.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * Update payment method for an order of the given user.
     */
    public Order updatePaymentMethodForUser(Long orderId, User user, String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new RuntimeException("Payment method is required.");
        }
        Order order = getOrderForUser(orderId, user);
        order.setPaymentMethod(paymentMethod);
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}


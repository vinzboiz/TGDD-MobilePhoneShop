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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public Order createOrderFromCart(
            User user,
            String shippingAddress,
            String phoneNumber,
            String paymentMethod,
            String note,
            int shippingFee
    ) {
        Cart cart = cartService.getActiveCart(user);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Ensure cart total is up to date
        cart.calculateTotal();
        if (cart.getTotalAmount() == null || cart.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cart total is invalid");
        }
        BigDecimal orderTotal = cart.getTotalAmount().add(BigDecimal.valueOf(shippingFee));

        // Check stock and prepare order details
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(orderTotal.doubleValue());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setPhoneNumber(phoneNumber);
        order.setPaymentMethod(paymentMethod);
        order.setNote(note);

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice().doubleValue());
            orderDetails.add(detail);
        }

        order.setOrderDetails(orderDetails);
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


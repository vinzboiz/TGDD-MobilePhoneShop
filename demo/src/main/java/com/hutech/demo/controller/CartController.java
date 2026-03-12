package com.hutech.demo.controller;

import com.hutech.demo.model.Cart;
import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.User;
import com.hutech.demo.model.Product;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.ProductService;
import com.hutech.demo.service.UserService;
import com.hutech.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    // session key
    private static final String SESSION_CART_KEY = "SESSION_CART";

    // simple in-memory cart representation for guests
    private static class SessionCart {
        Map<Long, Integer> items = new HashMap<>(); // productId -> quantity
    }

    // DTO for view (guest cart)
    public static class CartView {
        public static class Item {
            public Long id; // null for guest, use product.id for remove
            public Product product;
            public Integer quantity;
            public BigDecimal unitPrice;
            public BigDecimal subtotal;
        }
        public java.util.List<Item> items = new java.util.ArrayList<>();
        public BigDecimal totalAmount = BigDecimal.ZERO;
        public int totalQuantity = 0;
    }

    private SessionCart getSessionCart(HttpSession session) {
        SessionCart sc = (SessionCart) session.getAttribute(SESSION_CART_KEY);
        if (sc == null) {
            sc = new SessionCart();
            session.setAttribute(SESSION_CART_KEY, sc);
        }
        return sc;
    }

    private CartView buildCartView(SessionCart sc) {
        CartView view = new CartView();
        for (Map.Entry<Long,Integer> e : sc.items.entrySet()) {
            Product p = productService.findById(e.getKey()).orElse(null);
            if (p == null) continue;
            CartView.Item item = new CartView.Item();
            item.id = e.getKey();
            item.product = p;
            item.quantity = e.getValue();
            int discount = p.getDiscountPercent() != null ? p.getDiscountPercent() : 0;
            BigDecimal basePrice = BigDecimal.valueOf(p.getPrice());
            BigDecimal effectivePrice = basePrice
                    .multiply(BigDecimal.valueOf(100 - discount))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            item.unitPrice = effectivePrice;
            item.subtotal = effectivePrice.multiply(BigDecimal.valueOf(item.quantity));
            view.items.add(item);
            view.totalAmount = view.totalAmount.add(item.subtotal);
            view.totalQuantity += item.quantity;
        }
        return view;
    }

    /**
     * View cart page
     */
    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        User user = getCurrentUser();
        if (user != null) {
            Cart cart = cartService.getActiveCart(user);

            // Tính lại tạm tính theo quota khuyến mãi thay vì dùng unitPrice * quantity
            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            if (cart.getItems() != null) {
                for (CartItem item : cart.getItems()) {
                    Product p = item.getProduct();
                    if (p == null || p.getPrice() == null) continue;
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    int discountPercent = p.getDiscountPercent() != null ? p.getDiscountPercent() : 0;
                    java.math.BigDecimal basePrice = java.math.BigDecimal.valueOf(p.getPrice());

                    int promoRemain = p.getPromoRemaining() != null ? p.getPromoRemaining() : 0;
                    int discountedQty = (discountPercent > 0 && promoRemain > 0)
                            ? Math.min(quantity, promoRemain)
                            : 0;
                    int normalQty = quantity - discountedQty;

                    java.math.BigDecimal discountedPrice = basePrice
                            .multiply(java.math.BigDecimal.valueOf(100 - discountPercent))
                            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                    java.math.BigDecimal lineTotal = discountedPrice.multiply(java.math.BigDecimal.valueOf(discountedQty))
                            .add(basePrice.multiply(java.math.BigDecimal.valueOf(normalQty)));
                    subtotal = subtotal.add(lineTotal);
                }
            }
            cart.setTotalAmount(subtotal);
            model.addAttribute("cart", cart);
            String addr = user.getDefaultAddress();
            model.addAttribute("userDefaultAddress", addr != null && !addr.trim().isEmpty() ? addr : null);
            model.addAttribute("userRecipientName", user.getFullName());
            model.addAttribute("userRecipientPhone", user.getPhoneNumber());
            model.addAttribute("userRecipientGender", user.getGender() != null ? user.getGender() : "");

            int totalQty = cart.getItems() == null ? 0
                    : cart.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
            model.addAttribute("cartTotalQuantity", totalQty);

            int fee = OrderService.computeShippingFee(cart);
            model.addAttribute("shippingFeeVnd", fee);

            BigDecimal subtotalForPoints = cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
            int vipPoints = subtotalForPoints
                    .divide(new BigDecimal("10000"), 0, RoundingMode.DOWN)
                    .intValue();
            model.addAttribute("vipPoints", vipPoints);

            model.addAttribute("userLoggedIn", true);
        } else {
            SessionCart sc = getSessionCart(session);
            CartView cartView = buildCartView(sc);
            model.addAttribute("cart", cartView);
            model.addAttribute("cartTotalQuantity", cartView.totalQuantity);

            int fee = (cartView.totalAmount.compareTo(OrderService.FREE_SHIPPING_MIN_SUBTOTAL) >= 0
                    && cartView.totalQuantity >= OrderService.FREE_SHIPPING_MIN_TOTAL_QUANTITY)
                    ? 0 : OrderService.SHIPPING_FEE_VND;
            model.addAttribute("shippingFeeVnd", fee);
            int vipPoints = cartView.totalAmount
                    .divide(new BigDecimal("10000"), 0, RoundingMode.DOWN)
                    .intValue();
            model.addAttribute("vipPoints", vipPoints);
            model.addAttribute("userDefaultAddress", (String) null);
            model.addAttribute("userRecipientName", null);
            model.addAttribute("userRecipientPhone", null);
            model.addAttribute("userRecipientGender", null);
            model.addAttribute("userLoggedIn", false);
        }
        return "cart/cart";
    }

    /**
     * Add product to cart
     */
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                           @RequestParam(defaultValue = "1") Integer quantity,
                           @RequestParam(required = false) String returnTo,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        User user = getCurrentUser();
        try {
            if (user != null) {
                cartService.addToCart(user, productId, quantity);
            } else {
                SessionCart sc = getSessionCart(session);
                sc.items.merge(productId, quantity, Integer::sum);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (returnTo != null && !returnTo.isEmpty() && returnTo.startsWith("/")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/cart";
    }

    /**
     * Remove item from cart
     */
    @PostMapping("/remove/{cartItemId}")
    public String removeFromCart(@PathVariable Long cartItemId,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        User user = getCurrentUser();
        try {
            if (user != null) {
                cartService.removeFromCart(cartItemId);
            } else {
                // remove by productId stored as cartItemId in session scheme
                SessionCart sc = getSessionCart(session);
                sc.items.remove(cartItemId);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                "Item removed from cart");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Update item quantity
     */
    @PostMapping("/update/{cartItemId}")
    public String updateItemQuantity(@PathVariable Long cartItemId,
                                    @RequestParam Integer quantity,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        User user = getCurrentUser();
        try {
            if (user != null) {
                cartService.updateItemQuantity(cartItemId, quantity);
            } else {
                SessionCart sc = getSessionCart(session);
                if (sc.items.containsKey(cartItemId)) {
                    sc.items.put(cartItemId, quantity);
                }
            }
            redirectAttributes.addFlashAttribute("successMessage",
                "Cart updated successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Clear cart
     */
    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes, HttpSession session) {
        User user = getCurrentUser();
        try {
            if (user != null) {
                Cart cart = cartService.getActiveCart(user);
                cartService.clearCart(cart.getId());
            } else {
                session.removeAttribute(SESSION_CART_KEY);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                "Cart cleared successfully");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart";
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
            && !authentication.getPrincipal().equals("anonymousUser")) {
            String principal = authentication.getName();
            // principal is email; try email first, then username as fallback
            return userService.findByEmail(principal)
                    .orElseGet(() -> userService.findByUsername(principal).orElse(null));
        }
        return null;
    }
}
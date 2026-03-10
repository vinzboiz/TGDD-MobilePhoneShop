package com.hutech.demo.controller;

import com.hutech.demo.model.Cart;
import com.hutech.demo.model.Order;
import com.hutech.demo.model.User;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.OrderService;
import com.hutech.demo.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    @Data
    public static class CheckoutForm {
        private String shippingAddress;
        private String phoneNumber;
        private String paymentMethod;
        private String note;
        private Integer shippingFee;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model) {
        User user = getCurrentUser();
        Cart cart = cartService.getActiveCart(user);
        cart.calculateTotal();

        CheckoutForm form = new CheckoutForm();
        form.setPhoneNumber(user.getPhoneNumber());
        form.setShippingAddress(user.getDefaultAddress());
        form.setPaymentMethod("CASH_ON_DELIVERY");

        model.addAttribute("cart", cart);
        model.addAttribute("checkoutForm", form);
        return "orders/checkout";
    }

    @PostMapping("/checkout")
    public String doCheckout(@ModelAttribute("checkoutForm") CheckoutForm form,
                             RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            Order order = orderService.createOrderFromCart(
                    user,
                    form.getShippingAddress(),
                    form.getPhoneNumber(),
                    form.getPaymentMethod(),
                    form.getNote(),
                    form.getShippingFee() != null ? form.getShippingFee() : 0
            );
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully!");
            return "redirect:/orders/" + order.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        User user = getCurrentUser();
        Order order = orderService.getOrderForUser(id, user);
        model.addAttribute("order", order);
        return "orders/order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            orderService.cancelOrderForUser(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Đơn hàng đã được hủy.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/payment-method")
    public String updatePaymentMethod(@PathVariable Long id,
                                      @RequestParam("paymentMethod") String paymentMethod,
                                      RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        try {
            orderService.updatePaymentMethodForUser(id, user, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hình thức thanh toán thành công.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String principal = authentication.getName();
            return userService.findByEmail(principal)
                    .orElseGet(() -> userService.findByUsername(principal).orElse(null));
        }
        throw new IllegalStateException("User not authenticated");
    }
}


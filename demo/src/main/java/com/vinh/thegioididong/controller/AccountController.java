package com.hutech.demo.controller;

import com.hutech.demo.model.Order;
import com.hutech.demo.model.User;
import com.hutech.demo.model.UserVoucher;
import com.hutech.demo.service.OrderService;
import com.hutech.demo.service.UserService;
import com.hutech.demo.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final OrderService orderService;
    private final VoucherService voucherService;

    @GetMapping
    public String profile(Model model) {
        User user = getCurrentUser();
        List<Order> orders = orderService.getOrdersForUser(user);
        List<UserVoucher> vouchers = voucherService.getAvailableVouchers(user);

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("vouchers", vouchers);
        return "account/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phoneNumber,
                                @RequestParam String defaultAddress,
                                @RequestParam(required = false) String gender,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        userService.updateProfile(user, fullName, phoneNumber, defaultAddress, gender);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/account";
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


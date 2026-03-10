package com.hutech.demo.config;

import com.hutech.demo.model.Cart;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.model.User;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.ParentCategoryService;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;

/**
 * Thêm dữ liệu chung cho mọi view (header: parent_categories, cartCount, userDisplayLetter).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class LayoutControllerAdvice {

    private final ParentCategoryService parentCategoryService;
    private final CartService cartService;
    private final UserService userService;

    @ModelAttribute("parentCategories")
    public List<ParentCategory> addParentCategories() {
        try {
            return parentCategoryService.findAllForMenu();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @ModelAttribute("cartCount")
    public Integer addCartCount() {
        User user = getCurrentUser();
        if (user == null) return 0;
        try {
            Cart cart = cartService.getActiveCart(user);
            if (cart.getItems() == null) return 0;
            return cart.getItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("userDisplayLetter")
    public String addUserDisplayLetter() {
        User user = getCurrentUser();
        if (user == null) return null;
        String name = user.getFullName();
        if (name != null && !name.isBlank()) {
            return name.substring(0, 1).toUpperCase() + ".";
        }
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            return email.substring(0, 1).toUpperCase() + ".";
        }
        return "T";
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String principal = auth.getName();
        return userService.findByEmail(principal)
                .orElseGet(() -> userService.findByUsername(principal).orElse(null));
    }
}

package com.hutech.demo.controller;

import com.hutech.demo.model.User;
import com.hutech.demo.model.UserRole;
import com.hutech.demo.service.TotpService;
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

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/account/redeem")
@RequiredArgsConstructor
public class RedeemController {

    private final UserService userService;
    private final TotpService totpService;
    private final VoucherService voucherService;

    @GetMapping
    public String redeemPage(Model model, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null || user.getRole() != UserRole.CUSTOMER) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ khách hàng mới được đổi điểm tích lũy.");
            return "redirect:/account";
        }
        List<VoucherOptionDto> voucherOptions = List.of(
                new VoucherOptionDto(10_000, 100),
                new VoucherOptionDto(20_000, 200),
                new VoucherOptionDto(30_000, 300),
                new VoucherOptionDto(50_000, 500)
        );
        model.addAttribute("user", user);
        model.addAttribute("voucherOptions", voucherOptions);
        List<?> vouchers = voucherService.getAvailableVouchers(user);
        model.addAttribute("vouchers", vouchers != null ? vouchers : List.of());
        model.addAttribute("totpConfigured", Boolean.valueOf(user.getTotpSecret() != null && !user.getTotpSecret().isBlank()));
        model.addAttribute("showHeaderTopBar", false);
        return "account/redeem";
    }

    private static final String SESSION_TOTP_SETUP_SECRET = "totpSetupSecret";

    @GetMapping("/setup")
    public String setupPage(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        User user = requireCustomer();
        if (user.getTotpSecret() != null && !user.getTotpSecret().isBlank()) {
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã thiết lập Authenticator.");
            return "redirect:/account/redeem";
        }
        String secret = (String) model.asMap().get("setupSecret");
        if (secret == null || secret.isBlank()) {
            secret = totpService.generateSecret();
        }
        session.setAttribute(SESSION_TOTP_SETUP_SECRET, secret);
        String accountName = user.getEmail() != null ? user.getEmail() : (user.getUsername() != null ? user.getUsername() : "user");
        model.addAttribute("secret", secret);
        model.addAttribute("qrDataUrl", totpService.generateQrCodeDataUrl(totpService.getOtpAuthUrl(secret, accountName), 200, 200));
        model.addAttribute("user", user);
        model.addAttribute("showHeaderTopBar", false);
        return "account/redeem-setup";
    }

    @PostMapping("/setup")
    public String confirmSetup(@RequestParam(required = false) Integer totpCode,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        User user = requireCustomer();
        String secret = (String) session.getAttribute(SESSION_TOTP_SETUP_SECRET);
        if (secret == null || totpCode == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên thiết lập đã hết hạn. Vui lòng thử lại.");
            return "redirect:/account/redeem/setup";
        }
        if (!totpService.verify(secret, totpCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã xác thực không đúng. Vui lòng thử lại.");
            redirectAttributes.addFlashAttribute("setupSecret", secret);
            return "redirect:/account/redeem/setup";
        }
        session.removeAttribute(SESSION_TOTP_SETUP_SECRET);
        user.setTotpSecret(secret);
        userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Thiết lập Authenticator thành công.");
        return "redirect:/account/redeem";
    }

    @PostMapping
    public String redeem(@RequestParam int voucherValue,
                        @RequestParam int totpCode,
                        RedirectAttributes redirectAttributes) {
        User user = requireCustomer();
        try {
            var voucher = voucherService.redeem(user, voucherValue, totpCode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đổi điểm thành công! Bạn nhận được thẻ " + formatVnd(voucherValue) + " - Mã: " + voucher.getCode());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account/redeem";
    }

    private User requireCustomer() {
        User user = getCurrentUser();
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Chỉ khách hàng mới được đổi điểm tích lũy.");
        }
        return user;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("User not authenticated");
        }
        String principal = auth.getName();
        return userService.findByEmail(principal)
                .orElseGet(() -> userService.findByUsername(principal).orElseThrow(() -> new IllegalStateException("User not found")));
    }

    private String formatVnd(int v) {
        return String.format("%,d₫", v).replace(',', '.');
    }
}

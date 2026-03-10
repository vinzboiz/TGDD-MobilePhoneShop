package com.hutech.demo.controller;

import com.hutech.demo.model.Order;
import com.hutech.demo.model.User;
import com.hutech.demo.service.OrderService;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountRestController {

    private final UserService userService;
    private final OrderService orderService;

    public record ProfileDto(
            Long id,
            String email,
            String fullName,
            String phoneNumber,
            String defaultAddress,
            String gender,
            String role,
            String status,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            LocalDateTime lastLogin
    ) {}

    public record UpdateProfileRequest(
            String fullName,
            String phoneNumber,
            String defaultAddress,
            String gender
    ) {}

    public record OrderSummaryDto(
            Long id,
            LocalDateTime orderDate,
            Double totalAmount,
            String status
    ) {}

    @GetMapping("/profile")
    public ProfileDto getProfile(Principal principal) {
        User user = getCurrentUser(principal);
        return toProfileDto(user);
    }

    @PutMapping("/profile")
    public ProfileDto updateProfile(@RequestBody UpdateProfileRequest request, Principal principal) {
        User user = getCurrentUser(principal);
        User updated = userService.updateProfile(
                user,
                request.fullName(),
                request.phoneNumber(),
                request.defaultAddress(),
                request.gender()
        );
        return toProfileDto(updated);
    }

    @GetMapping("/orders")
    public List<OrderSummaryDto> getMyOrders(Principal principal) {
        User user = getCurrentUser(principal);
        List<Order> orders = orderService.getOrdersForUser(user);
        return orders.stream()
                .map(o -> new OrderSummaryDto(
                        o.getId(),
                        o.getOrderDate(),
                        o.getTotalAmount(),
                        o.getStatus() != null ? o.getStatus().name() : null
                ))
                .collect(Collectors.toList());
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String email = principal.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ProfileDto toProfileDto(User user) {
        return new ProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getDefaultAddress(),
                user.getGender(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getCreatedDate(),
                user.getUpdatedDate(),
                user.getLastLogin()
        );
    }
}


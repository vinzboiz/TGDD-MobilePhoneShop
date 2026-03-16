package com.hutech.demo.controller;

import com.hutech.demo.model.Order;
import com.hutech.demo.model.OrderDetail;
import com.hutech.demo.model.User;
import com.hutech.demo.service.OrderService;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;
    private final UserService userService;

    // DTOs
    public record CheckoutRequest(
            String shippingAddress,
            String phoneNumber,
            String paymentMethod,
            String note,
            Integer shippingFee
    ) {}

    public record OrderSummaryDto(
            Long id,
            LocalDateTime orderDate,
            Double totalAmount,
            String status
    ) {}

    public record OrderItemDto(
            Long productId,
            String productName,
            Double price,
            Integer quantity
    ) {}

    public record OrderDetailDto(
            Long id,
            LocalDateTime orderDate,
            Double totalAmount,
            String status,
            String shippingAddress,
            String phoneNumber,
            String paymentMethod,
            String note,
            List<OrderItemDto> items
    ) {}

    @PostMapping("/checkout")
    public ResponseEntity<OrderDetailDto> checkout(@RequestBody CheckoutRequest request, Principal principal) {
        User user = getCurrentUser(principal);
        Order order = orderService.createOrderFromCart(
                user,
                request.shippingAddress(),
                request.phoneNumber(),
                request.paymentMethod(),
                request.note(),
                request.shippingFee() != null ? request.shippingFee() : 0
        );
        return ResponseEntity.ok(toDetailDto(order));
    }

    @GetMapping("/my")
    public List<OrderSummaryDto> getMyOrders(Principal principal) {
        User user = getCurrentUser(principal);
        return orderService.getOrdersForUser(user).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDto> getOrder(@PathVariable Long id, Principal principal) {
        User user = getCurrentUser(principal);
        Order order = orderService.getOrderForUser(id, user);
        return ResponseEntity.ok(toDetailDto(order));
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String email = principal.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private OrderSummaryDto toSummaryDto(Order order) {
        return new OrderSummaryDto(
                order.getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus() != null ? order.getStatus().name() : null
        );
    }

    private OrderDetailDto toDetailDto(Order order) {
        List<OrderItemDto> items = order.getOrderDetails() == null ? List.of() :
                order.getOrderDetails().stream()
                        .map(this::toItemDto)
                        .collect(Collectors.toList());

        return new OrderDetailDto(
                order.getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getShippingAddress(),
                order.getPhoneNumber(),
                order.getPaymentMethod(),
                order.getNote(),
                items
        );
    }

    private OrderItemDto toItemDto(OrderDetail detail) {
        return new OrderItemDto(
                detail.getProduct().getId(),
                detail.getProduct().getName(),
                detail.getPrice(),
                detail.getQuantity()
        );
    }
}


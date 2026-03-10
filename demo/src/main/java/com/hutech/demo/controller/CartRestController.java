package com.hutech.demo.controller;

import com.hutech.demo.model.Cart;
import com.hutech.demo.model.CartItem;
import com.hutech.demo.model.User;
import com.hutech.demo.service.CartService;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartRestController {

    private final CartService cartService;
    private final UserService userService;

    public record CartItemDto(
            Long id,
            Long productId,
            String productName,
            Double price,
            Integer quantity,
            Double subtotal
    ) {}

    public record CartDto(
            Long id,
            List<CartItemDto> items,
            Double totalAmount
    ) {}

    public record AddItemRequest(
            Long productId,
            Integer quantity
    ) {}

    public record UpdateQuantityRequest(
            Integer quantity
    ) {}

    @GetMapping
    public CartDto getCart(Principal principal) {
        User user = getCurrentUser(principal);
        Cart cart = cartService.getActiveCart(user);
        cart.calculateTotal();
        return toDto(cart);
    }

    @PostMapping("/items")
    public CartDto addItem(@RequestBody AddItemRequest request, Principal principal) {
        User user = getCurrentUser(principal);
        Cart cart = cartService.addToCart(user, request.productId(), request.quantity());
        return toDto(cart);
    }

    @PutMapping("/items/{itemId}")
    public CartDto updateItem(
            @PathVariable Long itemId,
            @RequestBody UpdateQuantityRequest request,
            Principal principal
    ) {
        getCurrentUser(principal); // ensure authenticated
        Cart cart = cartService.updateItemQuantity(itemId, request.quantity());
        return toDto(cart);
    }

    @DeleteMapping("/items/{itemId}")
    public CartDto removeItem(@PathVariable Long itemId, Principal principal) {
        getCurrentUser(principal); // ensure authenticated
        Cart cart = cartService.removeFromCart(itemId);
        return toDto(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Principal principal) {
        User user = getCurrentUser(principal);
        Cart cart = cartService.getActiveCart(user);
        cartService.clearCart(cart.getId());
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String email = principal.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private CartDto toDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems() == null ? List.of() :
                cart.getItems().stream()
                        .map(this::toItemDto)
                        .collect(Collectors.toList());

        Double total = cart.getTotalAmount() != null
                ? cart.getTotalAmount().doubleValue()
                : itemDtos.stream()
                    .map(CartItemDto::subtotal)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .doubleValue();

        return new CartDto(
                cart.getId(),
                itemDtos,
                total
        );
    }

    private CartItemDto toItemDto(CartItem item) {
        Double unitPrice = item.getUnitPrice() != null
                ? item.getUnitPrice().doubleValue()
                : 0.0;
        Double subtotal = item.getSubtotal() != null
                ? item.getSubtotal().doubleValue()
                : unitPrice * item.getQuantity();

        return new CartItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                unitPrice,
                item.getQuantity(),
                subtotal
        );
    }
}


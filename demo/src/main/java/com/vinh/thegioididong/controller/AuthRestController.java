package com.hutech.demo.controller;

import com.hutech.demo.model.User;
import com.hutech.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final UserService userService;

    public record RegisterRequest(
            String email,
            String password,
            String confirmPassword,
            String phoneNumber
    ) {}

    public record UserDto(
            Long id,
            String email,
            String fullName,
            String phoneNumber,
            String defaultAddress,
            String role,
            String status
    ) {}

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        if (request.password() == null || !request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        User user = userService.register(request.email(), request.password(), request.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(user));
    }

    @GetMapping("/me")
    public UserDto me(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String email = principal.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return toDto(user);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getDefaultAddress(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null
        );
    }
}


package com.hutech.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password; // hashed với BCrypt

    @Column(length = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "0[0-9]{9}")
    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String defaultAddress;

    /** Giới tính xưng hô: "Anh" hoặc "Chị" */
    @Column(length = 10)
    private String gender;

    @Enumerated(EnumType.STRING)
    private UserRole role; // CUSTOMER, ADMIN

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, INACTIVE, BANNED

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    private LocalDateTime lastLogin;

    // Relationships
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Cart> carts;

    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Order> orders;
}
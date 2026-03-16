package com.hutech.demo.service;

import com.hutech.demo.model.User;
import com.hutech.demo.model.UserRole;
import com.hutech.demo.model.UserStatus;
import com.hutech.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(String email, String password, String phoneNumber) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(email); // use email as username for login
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedDate(LocalDateTime.now());
        user.setUpdatedDate(LocalDateTime.now());

        User saved = userRepository.save(user);
        System.out.println("[UserService] registered user id=" + saved.getId());
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateProfile(User user, String fullName, String phoneNumber, String defaultAddress, String gender) {
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setDefaultAddress(defaultAddress);
        if (gender != null && (gender.equals("Anh") || gender.equals("Chị"))) {
            user.setGender(gender);
        }
        user.setUpdatedDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    public boolean authenticate(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword()) && user.getStatus() == UserStatus.ACTIVE) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}
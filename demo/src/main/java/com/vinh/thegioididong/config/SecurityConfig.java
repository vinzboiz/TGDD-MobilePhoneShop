package com.hutech.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);

        http
            .requestCache(cache -> cache.requestCache(requestCache))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                        "/auth/**",
                        "/",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/categories/image/**"
                ).permitAll()
                // Admin: quản lý (khai báo trước để match trước)
                .requestMatchers(
                        "/admin/**",
                        "/categories/**",
                        "/products/add",
                        "/products/add/**",
                        "/products/edit/**",
                        "/products/update/**",
                        "/products/delete/**"
                ).hasRole("ADMIN")
                // Storefront: xem danh sách, chi tiết, ảnh — cho tất cả
                .requestMatchers(
                        "/products",
                        "/products/",
                        "/products/**",
                        "/products/image/**"
                ).permitAll()
                // Public API endpoints
                .requestMatchers(
                        "/api/auth/**",
                        "/api/products/**",
                        "/api/categories/**"
                ).permitAll()
                // Cart, account, orders APIs require authentication
                .requestMatchers(
                        "/api/cart/**",
                        "/api/account/**",
                        "/api/orders/**"
                ).authenticated()
                // Legacy MVC cart/account
                .requestMatchers("/cart/**", "/account/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/auth/login",
                        "/login",
                        "/api/**",
                        "/cart/**",
                        "/checkout/**",
                        "/orders/**",
                        "/account/update"
                )
            );

        return http.build();
    }
}

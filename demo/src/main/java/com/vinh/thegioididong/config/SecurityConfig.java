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
                        "/404",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/categories/image/**"
                ).permitAll()
                // Admin: full management (declare first to match before permitAll)
                .requestMatchers(
                        "/admin/**",
                        "/categories/delete/**",
                        "/products/delete/**"
                ).hasRole("ADMIN")
                // Manager: thêm/sửa danh mục và sản phẩm (không xóa)
                .requestMatchers("/categories/**").hasAnyRole("ADMIN", "MANAGER")
                // Manager: same as customer, but can manage products (view/add/edit) like admin (no delete)
                .requestMatchers(
                        "/products/add",
                        "/products/add/**",
                        "/products/edit/**",
                        "/products/update/**"
                ).hasAnyRole("ADMIN", "MANAGER")
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
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // Return 404 page instead of 403 for unauthorized access attempts
                    response.sendRedirect("/404");
                })
            );

        return http.build();
    }
}

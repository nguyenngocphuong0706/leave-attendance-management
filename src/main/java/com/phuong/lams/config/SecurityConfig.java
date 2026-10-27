package com.phuong.lams.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// Cấu hình: trang nào ai được vào, đăng nhập và đăng xuất ra sao
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Các đường dẫn ai cũng vào được (kể cả chưa đăng nhập)
                        .requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                        // Chỉ ADMIN vào được
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // MANAGER và ADMIN vào được
                        .requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        // Mọi đường dẫn còn lại: chỉ cần đã đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")             // dùng trang đăng nhập của ta
                        .defaultSuccessUrl("/", true)    // đăng nhập xong về trang chủ
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")        // đã đăng nhập nhưng không đủ quyền
                );

        return http.build();
    }
}
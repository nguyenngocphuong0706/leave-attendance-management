package com.phuong.lams.security;

import com.phuong.lams.entity.User;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Cho Spring Security biết cách tìm người dùng trong database theo email
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        // Spring cũng có class tên User, nên phải ghi đầy đủ tên package để không nhầm với entity User của ta
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name()) // EMPLOYEE -> quyền "ROLE_EMPLOYEE"
                .disabled(!user.isActive())   // tài khoản bị khóa thì không đăng nhập được
                .build();
    }
}
package com.phuong.lams.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Thêm các thông tin dùng chung (vai trò, tên người dùng) vào mọi trang giao diện
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return coQuyen("ROLE_ADMIN");
    }

    @ModelAttribute("isManager")
    public boolean isManager() {
        return coQuyen("ROLE_MANAGER") || coQuyen("ROLE_ADMIN");
    }

    @ModelAttribute("tenNguoiDung")
    public String tenNguoiDung() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    // Kiểm tra người đang đăng nhập có quyền này không
    private boolean coQuyen(String quyen) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(quyen));
    }
}
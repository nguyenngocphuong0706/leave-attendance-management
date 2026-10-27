package com.phuong.lams.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    // Hiện trang đăng nhập
    @GetMapping("/login")
    public String trangDangNhap() {
        return "login"; // tương ứng templates/login.html
    }

    // Hiện trang báo không đủ quyền
    @GetMapping("/403")
    public String khongCoQuyen() {
        return "error/403"; // tương ứng templates/error/403.html
    }
}
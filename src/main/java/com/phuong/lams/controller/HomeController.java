package com.phuong.lams.controller;

import com.phuong.lams.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String trangChu(Authentication authentication, Model model) {
        // authentication.getName() chính là email của người đang đăng nhập
        model.addAttribute("profile", userService.layThongTinCaNhan(authentication.getName()));
        return "dashboard";
    }
}
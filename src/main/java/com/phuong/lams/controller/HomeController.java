package com.phuong.lams.controller;

import com.phuong.lams.service.LeaveBalanceService;
import com.phuong.lams.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Year;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final LeaveBalanceService leaveBalanceService;

    @GetMapping("/")
    public String trangChu(Authentication authentication, Model model) {
        // authentication.getName() chính là email của người đang đăng nhập
        String email = authentication.getName();
        model.addAttribute("profile", userService.layThongTinCaNhan(email));
        model.addAttribute("soDu", leaveBalanceService.laySoDuNamNay(email));
        model.addAttribute("nam", Year.now().getValue());
        return "dashboard";
    }
}
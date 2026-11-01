package com.phuong.lams.controller;

import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Lệnh làm thêm giao cho chính người đang đăng nhập (mọi vai trò đều dùng được)
@Controller
@RequestMapping("/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private static final String DANH_SACH = "redirect:/overtime";

    private final OvertimeService overtimeService;

    @GetMapping
    public String danhSach(Authentication authentication, Model model) {
        model.addAttribute("danhSach", overtimeService.layCuaToi(authentication.getName()));
        return "overtime/mine";
    }

    @PostMapping("/{id}/accept")
    public String dongY(@PathVariable Long id,
                        @RequestParam(required = false) String ghiChu,
                        Authentication authentication, RedirectAttributes ra) {
        try {
            overtimeService.phanHoi(authentication.getName(), id, true, ghiChu);
            ra.addFlashAttribute("thanhCong", "Bạn đã đồng ý làm thêm theo lệnh");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    @PostMapping("/{id}/decline")
    public String tuChoi(@PathVariable Long id,
                         @RequestParam(required = false) String ghiChu,
                         Authentication authentication, RedirectAttributes ra) {
        try {
            overtimeService.phanHoi(authentication.getName(), id, false, ghiChu);
            ra.addFlashAttribute("thanhCong", "Bạn đã từ chối lệnh làm thêm");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }
}
package com.phuong.lams.controller;

import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;

// Chấm công của chính người đang đăng nhập (mọi vai trò đều dùng được)
@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final String TRANG_CHAM_CONG = "redirect:/attendance";

    private final AttendanceService attendanceService;

    // Trang chấm công: trạng thái hôm nay và lịch sử của một tháng (mặc định tháng hiện tại)
    @GetMapping
    public String trangChamCong(@RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM") YearMonth thang,
                                Authentication authentication, Model model) {
        YearMonth thangXem = thang != null ? thang : attendanceService.thangHienTai();
        model.addAttribute("homNay", attendanceService.layHomNay(authentication.getName()));
        model.addAttribute("lichSu", attendanceService.layLichSu(authentication.getName(), thangXem));
        // Dạng năm-tháng để điền lại vào ô chọn tháng
        model.addAttribute("thang", thangXem.toString());
        return "attendance/index";
    }

    @PostMapping("/check-in")
    public String checkIn(Authentication authentication, RedirectAttributes ra) {
        try {
            attendanceService.checkIn(authentication.getName());
            ra.addFlashAttribute("thanhCong", "Đã check-in");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return TRANG_CHAM_CONG;
    }

    @PostMapping("/check-out")
    public String checkOut(Authentication authentication, RedirectAttributes ra) {
        try {
            attendanceService.checkOut(authentication.getName());
            ra.addFlashAttribute("thanhCong", "Đã check-out");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return TRANG_CHAM_CONG;
    }
}
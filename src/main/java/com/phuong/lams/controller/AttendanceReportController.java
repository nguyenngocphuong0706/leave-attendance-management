package com.phuong.lams.controller;

import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.AttendanceReportService;
import com.phuong.lams.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;

// Báo cáo chấm công tháng. Mọi vai trò đều vào được, phạm vi dữ liệu do Service quyết định theo vai trò
@Controller
@RequestMapping("/reports/attendance")
@RequiredArgsConstructor
public class AttendanceReportController {

    private final AttendanceReportService reportService;
    private final AttendanceService attendanceService;

    @GetMapping
    public String baoCao(@RequestParam(required = false)
                         @DateTimeFormat(pattern = "yyyy-MM") YearMonth thang,
                         Authentication authentication, Model model) {
        YearMonth thangXem = thang != null ? thang : attendanceService.thangHienTai();
        model.addAttribute("baoCao", reportService.layBaoCao(authentication.getName(), thangXem));
        // Dạng năm-tháng để điền lại vào ô chọn tháng và ghép vào đường dẫn chi tiết
        model.addAttribute("thangIso", thangXem.toString());
        return "report/attendance";
    }

    @GetMapping("/{userId}")
    public String chiTiet(@PathVariable Long userId,
                          @RequestParam(required = false)
                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth thang,
                          Authentication authentication, Model model, RedirectAttributes ra) {
        YearMonth thangXem = thang != null ? thang : attendanceService.thangHienTai();
        try {
            model.addAttribute("chiTiet", reportService.layChiTiet(authentication.getName(), userId, thangXem));
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
            return "redirect:/reports/attendance";
        }
        model.addAttribute("thangIso", thangXem.toString());
        model.addAttribute("userId", userId);
        return "report/attendance-detail";
    }
}
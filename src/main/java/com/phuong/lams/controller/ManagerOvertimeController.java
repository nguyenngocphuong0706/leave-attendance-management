package com.phuong.lams.controller;

import com.phuong.lams.dto.OvertimeOrderForm;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.OvertimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Giao việc làm thêm (chỉ MANAGER và ADMIN, đã được chặn bởi SecurityConfig với đường dẫn /manager/**)
@Controller
@RequestMapping("/manager/overtime")
@RequiredArgsConstructor
public class ManagerOvertimeController {

    private static final String DANH_SACH = "redirect:/manager/overtime";
    private static final String FORM = "manager/overtime/form";

    private final OvertimeService overtimeService;

    // Các lệnh do tôi tạo
    @GetMapping
    public String danhSach(Authentication authentication, Model model) {
        model.addAttribute("danhSach", overtimeService.layDaTao(authentication.getName()));
        return "manager/overtime/list";
    }

    @GetMapping("/new")
    public String formThem(Authentication authentication, Model model) {
        model.addAttribute("form", new OvertimeOrderForm());
        model.addAttribute("nhanVien", overtimeService.layNhanVienDuocGiao(authentication.getName()));
        return FORM;
    }

    @PostMapping
    public String tao(@Valid @ModelAttribute("form") OvertimeOrderForm form,
                      BindingResult result, Authentication authentication,
                      Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                overtimeService.taoLenh(authentication.getName(), form);
                ra.addFlashAttribute("thanhCong", "Đã gửi lệnh làm thêm, đang chờ nhân viên phản hồi");
                return DANH_SACH;
            } catch (BusinessException e) {
                if (e.getField() != null) {
                    result.rejectValue(e.getField(), "nghiepVu", e.getMessage());
                } else {
                    result.reject("nghiepVu", e.getMessage());
                }
            }
        }
        model.addAttribute("nhanVien", overtimeService.layNhanVienDuocGiao(authentication.getName()));
        return FORM;
    }

    @PostMapping("/{id}/cancel")
    public String huy(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            overtimeService.huyLenh(authentication.getName(), id);
            ra.addFlashAttribute("thanhCong", "Đã hủy lệnh làm thêm");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }
}
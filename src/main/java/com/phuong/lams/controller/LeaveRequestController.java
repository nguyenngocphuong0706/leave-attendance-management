package com.phuong.lams.controller;

import com.phuong.lams.dto.LeaveRequestForm;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.LeaveRequestService;
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

// Đơn xin nghỉ phép của chính người đang đăng nhập (mọi vai trò đều dùng được)
@Controller
@RequestMapping("/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private static final String DANH_SACH = "redirect:/leave-requests";
    private static final String FORM = "leave/form";

    private final LeaveRequestService leaveRequestService;

    // Danh sách đơn của tôi
    @GetMapping
    public String danhSach(Authentication authentication, Model model) {
        model.addAttribute("danhSach", leaveRequestService.layCuaToi(authentication.getName()));
        return "leave/list";
    }

    // Hiện form xin nghỉ
    @GetMapping("/new")
    public String formThem(Authentication authentication, Model model) {
        model.addAttribute("form", new LeaveRequestForm());
        model.addAttribute("loaiNghi", leaveRequestService.layLoaiNghiChoPhep(authentication.getName()));
        return FORM;
    }

    // Xử lý gửi đơn
    @PostMapping
    public String gui(@Valid @ModelAttribute("form") LeaveRequestForm form,
                      BindingResult result, Authentication authentication,
                      Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                leaveRequestService.taoDon(authentication.getName(), form);
                ra.addFlashAttribute("thanhCong", "Đã gửi đơn xin nghỉ, đang chờ quản lý duyệt");
                return DANH_SACH;
            } catch (BusinessException e) {
                if (e.getField() != null) {
                    result.rejectValue(e.getField(), "nghiepVu", e.getMessage());
                } else {
                    result.reject("nghiepVu", e.getMessage());
                }
            }
        }
        model.addAttribute("loaiNghi", leaveRequestService.layLoaiNghiChoPhep(authentication.getName()));
        return FORM;
    }

    // Hủy đơn đang chờ duyệt
    @PostMapping("/{id}/cancel")
    public String huy(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            leaveRequestService.huyDon(authentication.getName(), id);
            ra.addFlashAttribute("thanhCong", "Đã hủy đơn");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }
}
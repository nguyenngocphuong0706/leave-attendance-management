package com.phuong.lams.controller;

import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.LeaveApprovalService;
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

// Duyệt đơn nghỉ phép (chỉ MANAGER và ADMIN, đã được chặn bởi SecurityConfig với đường dẫn /manager/**)
@Controller
@RequestMapping("/manager/leave-requests")
@RequiredArgsConstructor
public class ManagerLeaveRequestController {

    private static final String DANH_SACH = "redirect:/manager/leave-requests";

    private final LeaveApprovalService leaveApprovalService;

    // Danh sách đơn đang chờ mà tôi có quyền duyệt
    @GetMapping
    public String danhSach(Authentication authentication, Model model) {
        model.addAttribute("danhSach", leaveApprovalService.layChoDuyet(authentication.getName()));
        return "manager/leave-request/list";
    }

    // Duyệt đơn
    @PostMapping("/{id}/approve")
    public String duyet(@PathVariable Long id,
                        @RequestParam(required = false) String ghiChu,
                        Authentication authentication, RedirectAttributes ra) {
        try {
            leaveApprovalService.duyet(authentication.getName(), id, ghiChu);
            ra.addFlashAttribute("thanhCong", "Đã duyệt đơn và trừ ngày phép");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    // Từ chối đơn
    @PostMapping("/{id}/reject")
    public String tuChoi(@PathVariable Long id,
                         @RequestParam(required = false) String ghiChu,
                         Authentication authentication, RedirectAttributes ra) {
        try {
            leaveApprovalService.tuChoi(authentication.getName(), id, ghiChu);
            ra.addFlashAttribute("thanhCong", "Đã từ chối đơn");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }
}
package com.phuong.lams.controller;

import com.phuong.lams.dto.LeaveTypeForm;
import com.phuong.lams.enums.Applicability;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.LeaveTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Quản lý loại nghỉ phép (chỉ ADMIN, đã được chặn bởi SecurityConfig với đường dẫn /admin/**)
@Controller
@RequestMapping("/admin/leave-types")
@RequiredArgsConstructor
public class AdminLeaveTypeController {

    private static final String DANH_SACH = "redirect:/admin/leave-types";
    private static final String FORM = "admin/leave-type/form";

    private final LeaveTypeService leaveTypeService;

    // Hiện danh sách
    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("danhSach", leaveTypeService.layDanhSach());
        return "admin/leave-type/list";
    }

    // Hiện form thêm mới
    @GetMapping("/new")
    public String formThem(Model model) {
        model.addAttribute("form", new LeaveTypeForm());
        datThongTinForm(model, "Thêm loại nghỉ phép", "/admin/leave-types");
        return FORM;
    }

    // Xử lý thêm mới
    @PostMapping
    public String them(@Valid @ModelAttribute("form") LeaveTypeForm form,
                       BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                leaveTypeService.them(form);
                ra.addFlashAttribute("thanhCong", "Đã thêm loại nghỉ phép và cấp số ngày cho đúng đối tượng");
                return DANH_SACH;
            } catch (BusinessException e) {
                ganLoi(result, e);
            }
        }
        datThongTinForm(model, "Thêm loại nghỉ phép", "/admin/leave-types");
        return FORM;
    }

    // Hiện form sửa
    @GetMapping("/{id}/edit")
    public String formSua(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("form", leaveTypeService.layDeSua(id));
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
            return DANH_SACH;
        }
        datThongTinForm(model, "Sửa loại nghỉ phép", "/admin/leave-types/" + id);
        return FORM;
    }

    // Xử lý sửa
    @PostMapping("/{id}")
    public String capNhat(@PathVariable Long id,
                          @Valid @ModelAttribute("form") LeaveTypeForm form,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                leaveTypeService.capNhat(id, form);
                ra.addFlashAttribute("thanhCong", "Đã cập nhật loại nghỉ phép");
                return DANH_SACH;
            } catch (BusinessException e) {
                ganLoi(result, e);
            }
        }
        datThongTinForm(model, "Sửa loại nghỉ phép", "/admin/leave-types/" + id);
        return FORM;
    }

    // Ngừng dùng hoặc dùng lại
    @PostMapping("/{id}/toggle")
    public String doiTrangThai(@PathVariable Long id, RedirectAttributes ra) {
        try {
            boolean dangDung = leaveTypeService.doiTrangThai(id);
            ra.addFlashAttribute("thanhCong", dangDung ? "Đã dùng lại loại nghỉ" : "Đã ngừng dùng loại nghỉ");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    // Xóa loại nghỉ (chỉ được khi chưa ai dùng)
    @PostMapping("/{id}/delete")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        try {
            leaveTypeService.xoa(id);
            ra.addFlashAttribute("thanhCong", "Đã xóa loại nghỉ");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    private void datThongTinForm(Model model, String tieuDe, String action) {
        model.addAttribute("tieuDe", tieuDe);
        model.addAttribute("action", action);
        model.addAttribute("doiTuong", Applicability.values());
    }

    // Lỗi có tên ô thì gắn vào ô đó, không thì thành lỗi chung của form
    private void ganLoi(BindingResult result, BusinessException e) {
        if (e.getField() != null) {
            result.rejectValue(e.getField(), "nghiepVu", e.getMessage());
        } else {
            result.reject("nghiepVu", e.getMessage());
        }
    }
}
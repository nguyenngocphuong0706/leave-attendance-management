package com.phuong.lams.controller;

import com.phuong.lams.dto.UserForm;
import com.phuong.lams.enums.Gender;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.DepartmentService;
import com.phuong.lams.service.UserManagementService;
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

// Quản lý người dùng (chỉ ADMIN, đã được chặn bởi SecurityConfig với đường dẫn /admin/**)
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final String DANH_SACH = "redirect:/admin/users";
    private static final String FORM = "admin/user/form";

    private final UserManagementService userService;
    private final DepartmentService departmentService;

    // Hiện danh sách
    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("danhSach", userService.layDanhSach());
        return "admin/user/list";
    }

    // Hiện form thêm mới
    @GetMapping("/new")
    public String formThem(Model model) {
        model.addAttribute("form", new UserForm());
        datThongTinForm(model, "Thêm người dùng", "/admin/users", true, null);
        return FORM;
    }

    // Xử lý thêm mới
    @PostMapping
    public String them(@Valid @ModelAttribute("form") UserForm form,
                       BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                userService.them(form);
                ra.addFlashAttribute("thanhCong", "Đã thêm người dùng");
                return DANH_SACH;
            } catch (BusinessException e) {
                ganLoi(result, e);
            }
        }
        datThongTinForm(model, "Thêm người dùng", "/admin/users", true, null);
        return FORM;
    }

    // Hiện form sửa
    @GetMapping("/{id}/edit")
    public String formSua(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("form", userService.layDeSua(id));
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
            return DANH_SACH;
        }
        datThongTinForm(model, "Sửa người dùng", "/admin/users/" + id, false, id);
        return FORM;
    }

    // Xử lý sửa
    @PostMapping("/{id}")
    public String capNhat(@PathVariable Long id,
                          @Valid @ModelAttribute("form") UserForm form,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                userService.capNhat(id, form);
                ra.addFlashAttribute("thanhCong", "Đã cập nhật người dùng");
                return DANH_SACH;
            } catch (BusinessException e) {
                ganLoi(result, e);
            }
        }
        datThongTinForm(model, "Sửa người dùng", "/admin/users/" + id, false, id);
        return FORM;
    }

    // Khóa hoặc mở khóa tài khoản
    @PostMapping("/{id}/toggle")
    public String doiTrangThai(@PathVariable Long id, Authentication authentication, RedirectAttributes ra) {
        try {
            boolean hoatDong = userService.doiTrangThai(id, authentication.getName());
            ra.addFlashAttribute("thanhCong", hoatDong ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    // Đưa dữ liệu dùng chung cho form: tiêu đề, đường dẫn gửi form, các danh sách để chọn
    private void datThongTinForm(Model model, String tieuDe, String action, boolean laThemMoi, Long loaiTruId) {
        model.addAttribute("tieuDe", tieuDe);
        model.addAttribute("action", action);
        model.addAttribute("laThemMoi", laThemMoi);
        model.addAttribute("gioiTinh", Gender.values());
        model.addAttribute("vaiTro", Role.values());
        model.addAttribute("phongBan", departmentService.layDanhSach());
        model.addAttribute("quanLy", userService.layDanhSachQuanLy(loaiTruId));
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
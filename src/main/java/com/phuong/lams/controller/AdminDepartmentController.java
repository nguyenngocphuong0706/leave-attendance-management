package com.phuong.lams.controller;

import com.phuong.lams.dto.DepartmentForm;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.service.DepartmentService;
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

// Quản lý phòng ban (chỉ ADMIN, đã được chặn bởi SecurityConfig với đường dẫn /admin/**)
@Controller
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private static final String DANH_SACH = "redirect:/admin/departments";

    private final DepartmentService departmentService;

    // Hiện danh sách
    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("danhSach", departmentService.layDanhSach());
        return "admin/department/list";
    }

    // Hiện form thêm mới
    @GetMapping("/new")
    public String formThem(Model model) {
        model.addAttribute("form", new DepartmentForm());
        datThongTinForm(model, "Thêm phòng ban", "/admin/departments");
        return "admin/department/form";
    }

    // Xử lý thêm mới
    @PostMapping
    public String them(@Valid @ModelAttribute("form") DepartmentForm form,
                       BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                departmentService.them(form);
                ra.addFlashAttribute("thanhCong", "Đã thêm phòng ban");
                return DANH_SACH;
            } catch (BusinessException e) {
                result.rejectValue("name", "trung", e.getMessage());
            }
        }
        datThongTinForm(model, "Thêm phòng ban", "/admin/departments");
        return "admin/department/form";
    }

    // Hiện form sửa
    @GetMapping("/{id}/edit")
    public String formSua(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("form", departmentService.layDeSua(id));
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
            return DANH_SACH;
        }
        datThongTinForm(model, "Sửa phòng ban", "/admin/departments/" + id);
        return "admin/department/form";
    }

    // Xử lý sửa
    @PostMapping("/{id}")
    public String capNhat(@PathVariable Long id,
                          @Valid @ModelAttribute("form") DepartmentForm form,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                departmentService.capNhat(id, form);
                ra.addFlashAttribute("thanhCong", "Đã cập nhật phòng ban");
                return DANH_SACH;
            } catch (BusinessException e) {
                result.rejectValue("name", "trung", e.getMessage());
            }
        }
        datThongTinForm(model, "Sửa phòng ban", "/admin/departments/" + id);
        return "admin/department/form";
    }

    // Xử lý xóa
    @PostMapping("/{id}/delete")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        try {
            departmentService.xoa(id);
            ra.addFlashAttribute("thanhCong", "Đã xóa phòng ban");
        } catch (BusinessException e) {
            ra.addFlashAttribute("loi", e.getMessage());
        }
        return DANH_SACH;
    }

    private void datThongTinForm(Model model, String tieuDe, String action) {
        model.addAttribute("tieuDe", tieuDe);
        model.addAttribute("action", action);
    }
}
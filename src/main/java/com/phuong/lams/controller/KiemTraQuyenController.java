package com.phuong.lams.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// TẠM THỜI: chỉ để kiểm tra phân quyền, sẽ xóa khi làm trang thật
@RestController
public class KiemTraQuyenController {

    @GetMapping("/admin/test")
    public String trangAdmin() {
        return "Đây là trang chỉ dành cho ADMIN";
    }

    @GetMapping("/manager/test")
    public String trangQuanLy() {
        return "Đây là trang dành cho MANAGER và ADMIN";
    }
}
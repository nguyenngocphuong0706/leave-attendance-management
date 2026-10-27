package com.phuong.lams.dto;

import com.phuong.lams.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Thông tin cá nhân hiển thị trên trang chủ
@Getter
@AllArgsConstructor
public class UserProfile {
    private final String fullName;
    private final String email;
    private final Role role;
    private final String departmentName;
    private final String managerName;
}
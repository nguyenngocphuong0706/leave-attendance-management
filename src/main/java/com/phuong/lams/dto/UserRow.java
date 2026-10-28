package com.phuong.lams.dto;

import com.phuong.lams.enums.Role;

// Một dòng trong danh sách người dùng
public record UserRow(Long id, String fullName, String email, Role role,
                      String departmentName, String managerName, boolean active) {
}
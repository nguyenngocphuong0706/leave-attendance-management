package com.phuong.lams.dto;

// Một dòng trong danh sách phòng ban: mã, tên, số nhân viên
public record DepartmentRow(Long id, String name, Long soNhanVien) {
}
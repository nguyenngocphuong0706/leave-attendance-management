package com.phuong.lams.dto;

// Một dòng trong danh sách loại nghỉ phép
public record LeaveTypeRow(Long id, String name, int daysPerYear, String appliesToLabel, boolean active) {
}
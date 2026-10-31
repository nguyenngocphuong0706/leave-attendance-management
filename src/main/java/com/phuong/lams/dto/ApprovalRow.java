package com.phuong.lams.dto;

// Một dòng trong danh sách đơn chờ duyệt của người quản lý
public record ApprovalRow(Long id, String employeeName, String departmentName, String leaveTypeName,
                          String startDate, String endDate, int days, String reason, String createdAt) {
}
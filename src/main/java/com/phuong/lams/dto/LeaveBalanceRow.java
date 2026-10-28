package com.phuong.lams.dto;

// Một dòng số dư phép hiển thị trên trang chủ
public record LeaveBalanceRow(String leaveTypeName, int totalDays, int usedDays, int remainingDays) {
}
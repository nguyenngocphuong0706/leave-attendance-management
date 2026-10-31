package com.phuong.lams.dto;

// Một dòng trong danh sách đơn nghỉ phép. Ngày đã được định dạng sẵn để giao diện chỉ việc hiển thị
public record LeaveRequestRow(Long id, String leaveTypeName, String startDate, String endDate,
                              int days, String reason, String statusLabel, String statusBadge,
                              String reviewNote, boolean coTheHuy) {
}
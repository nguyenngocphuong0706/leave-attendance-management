package com.phuong.lams.dto;

// Một dòng trong danh sách lệnh làm thêm. Ngày giờ đã được định dạng sẵn
public record OvertimeRow(Long id, String employeeName, String creatorName, String ngay, String thu,
                          String khungGio, String soGio, String reason, String statusLabel,
                          String statusBadge, String responseNote, boolean coTheTraLoi, boolean coTheHuy) {
}
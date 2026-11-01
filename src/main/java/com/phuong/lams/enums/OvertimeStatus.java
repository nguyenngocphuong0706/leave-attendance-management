package com.phuong.lams.enums;

// Trạng thái của một lệnh làm thêm giờ
public enum OvertimeStatus {
    PENDING("Chờ phản hồi", "warning"),
    ACCEPTED("Đã đồng ý", "success"),
    DECLINED("Đã từ chối", "danger"),
    CANCELLED("Đã hủy", "secondary");

    private final String label;
    private final String badge;

    OvertimeStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }
}
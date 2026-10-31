package com.phuong.lams.enums;

// Trạng thái của một đơn xin nghỉ phép
public enum LeaveStatus {
    PENDING("Chờ duyệt", "warning"),
    APPROVED("Đã duyệt", "success"),
    REJECTED("Từ chối", "danger"),
    CANCELLED("Đã hủy", "secondary");

    private final String label;
    private final String badge; // màu của nhãn Bootstrap

    LeaveStatus(String label, String badge) {
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
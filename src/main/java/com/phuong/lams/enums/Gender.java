package com.phuong.lams.enums;

// Giới tính của người dùng
public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    // Chữ hiển thị trên giao diện
    public String getLabel() {
        return label;
    }
}
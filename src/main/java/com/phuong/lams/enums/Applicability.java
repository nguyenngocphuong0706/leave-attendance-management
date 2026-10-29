package com.phuong.lams.enums;

// Đối tượng áp dụng của một loại nghỉ phép
public enum Applicability {
    ALL("Tất cả"),
    MALE("Chỉ nam"),
    FEMALE("Chỉ nữ");

    private final String label;

    Applicability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // Quy tắc "loại nghỉ này có áp dụng cho người này không" nằm duy nhất ở đây.
    // Người chưa có giới tính chỉ được hưởng loại nghỉ áp dụng cho tất cả.
    public boolean apDungCho(Gender gioiTinh) {
        return switch (this) {
            case ALL -> true;
            case MALE -> gioiTinh == Gender.MALE;
            case FEMALE -> gioiTinh == Gender.FEMALE;
        };
    }
}
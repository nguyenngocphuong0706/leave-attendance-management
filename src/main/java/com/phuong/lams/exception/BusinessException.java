package com.phuong.lams.exception;

// Lỗi nghiệp vụ: vi phạm quy tắc của hệ thống (trùng tên, còn nhân viên...)
public class BusinessException extends RuntimeException {

    // Tên ô nhập bị lỗi, null nếu lỗi không thuộc ô nào
    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
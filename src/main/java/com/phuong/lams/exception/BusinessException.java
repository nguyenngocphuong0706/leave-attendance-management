package com.phuong.lams.exception;

// Lỗi nghiệp vụ: vi phạm quy tắc của hệ thống (trùng tên, còn nhân viên...)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
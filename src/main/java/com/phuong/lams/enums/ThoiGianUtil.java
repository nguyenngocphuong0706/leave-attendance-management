package com.phuong.lams.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;

// Các hàm tiện ích về ngày giờ dùng chung
public final class ThoiGianUtil {

    private ThoiGianUtil() {
    }

    public static boolean laCuoiTuan(LocalDate ngay) {
        DayOfWeek thu = ngay.getDayOfWeek();
        return thu == DayOfWeek.SATURDAY || thu == DayOfWeek.SUNDAY;
    }

    public static String tenThu(DayOfWeek thu) {
        return switch (thu) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    // Ví dụ: 4 giờ 05 phút
    public static String dinhDangSoGio(Duration khoang) {
        long phut = khoang.toMinutes();
        return (phut / 60) + " giờ " + String.format("%02d", phut % 60) + " phút";
    }
}
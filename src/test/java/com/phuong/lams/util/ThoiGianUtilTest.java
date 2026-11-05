package com.phuong.lams.util;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThoiGianUtilTest {

    @Test
    void laCuoiTuan_thuBayVaChuNhatLaCuoiTuan() {
        assertTrue(ThoiGianUtil.laCuoiTuan(LocalDate.of(2026, 11, 7)));   // thứ Bảy
        assertTrue(ThoiGianUtil.laCuoiTuan(LocalDate.of(2026, 11, 8)));   // Chủ nhật
    }

    @Test
    void laCuoiTuan_ngayThuongKhongPhaiCuoiTuan() {
        assertFalse(ThoiGianUtil.laCuoiTuan(LocalDate.of(2026, 11, 2)));  // thứ Hai
        assertFalse(ThoiGianUtil.laCuoiTuan(LocalDate.of(2026, 11, 6)));  // thứ Sáu
    }

    @Test
    void tenThu_traVeTenTiengViet() {
        assertEquals("Thứ Hai", ThoiGianUtil.tenThu(DayOfWeek.MONDAY));
        assertEquals("Thứ Bảy", ThoiGianUtil.tenThu(DayOfWeek.SATURDAY));
        assertEquals("Chủ nhật", ThoiGianUtil.tenThu(DayOfWeek.SUNDAY));
    }

    @Test
    void dinhDangSoGio_giuHaiChuSoChoPhut() {
        assertEquals("3 giờ 55 phút", ThoiGianUtil.dinhDangSoGio(Duration.ofMinutes(235)));
        assertEquals("2 giờ 05 phút", ThoiGianUtil.dinhDangSoGio(Duration.ofMinutes(125)));
        assertEquals("0 giờ 00 phút", ThoiGianUtil.dinhDangSoGio(Duration.ZERO));
    }
}
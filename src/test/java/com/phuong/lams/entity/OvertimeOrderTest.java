package com.phuong.lams.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Kiểm tra cách tính giờ làm thêm: chỉ tính phần nằm trong khung giờ của lệnh
class OvertimeOrderTest {

    private static final LocalDate NGAY = LocalDate.of(2026, 11, 7);

    private OvertimeOrder lenhTu8Den12() {
        OvertimeOrder lenh = new OvertimeOrder();
        lenh.setWorkDate(NGAY);
        lenh.setStartTime(LocalTime.of(8, 0));
        lenh.setEndTime(LocalTime.of(12, 0));
        return lenh;
    }

    private LocalDateTime luc(int gio, int phut) {
        return LocalDateTime.of(NGAY, LocalTime.of(gio, phut));
    }

    @Test
    void lamVaoTreVaRaMuon_chiTinhPhanTrongKhung() {
        // Vào 08:05, ra 12:10 thì chỉ tính từ 08:05 đến 12:00 là 3 giờ 55 phút
        assertEquals(Duration.ofMinutes(235), lenhTu8Den12().thoiGianTrongKhung(luc(8, 5), luc(12, 10)));
    }

    @Test
    void vaoSomHonKhung_tinhTuGioBatDauCuaLenh() {
        // Vào 07:30 nhưng lệnh bắt đầu 08:00, ra 11:00 thì tính 3 giờ
        assertEquals(Duration.ofHours(3), lenhTu8Den12().thoiGianTrongKhung(luc(7, 30), luc(11, 0)));
    }

    @Test
    void lamDungKhung_tinhDayDu() {
        assertEquals(Duration.ofHours(4), lenhTu8Den12().thoiGianTrongKhung(luc(8, 0), luc(12, 0)));
    }

    @Test
    void lamHoanToanNgoaiKhung_khongTinhGio() {
        assertEquals(Duration.ZERO, lenhTu8Den12().thoiGianTrongKhung(luc(13, 0), luc(14, 0)));
    }

    @Test
    void vaoVaRaCungLuc_khongTinhGio() {
        assertEquals(Duration.ZERO, lenhTu8Den12().thoiGianTrongKhung(luc(9, 0), luc(9, 0)));
    }
}
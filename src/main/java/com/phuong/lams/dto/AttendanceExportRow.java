package com.phuong.lams.dto;

import java.time.LocalDate;

// Một dòng trong sheet Chi tiết của file Excel: chấm công một người trong một ngày
public record AttendanceExportRow(String hoTen, LocalDate ngay, String thu, String gioVao, String gioRa,
                                  Double soGioLam, Double gioLamThemTinh, String ghiChu) {
}
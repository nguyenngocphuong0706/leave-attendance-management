package com.phuong.lams.dto;

import java.util.List;

// Báo cáo chấm công tháng: phạm vi người xem, số ngày làm việc của tháng và các dòng chi tiết
public record AttendanceReport(String thang, String phamVi, int soNgayLamViec, List<AttendanceReportRow> dong) {
}
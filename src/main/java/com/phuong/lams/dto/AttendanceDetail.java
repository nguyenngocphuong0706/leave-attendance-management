package com.phuong.lams.dto;

import java.util.List;

// Chi tiết chấm công từng ngày của một người trong một tháng
public record AttendanceDetail(String hoTen, String phongBan, String thang, List<AttendanceRow> dong) {
}
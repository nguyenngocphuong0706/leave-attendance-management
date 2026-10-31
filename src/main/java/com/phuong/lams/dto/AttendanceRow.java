package com.phuong.lams.dto;

// Một dòng trong lịch sử chấm công. Ngày giờ đã được định dạng sẵn
public record AttendanceRow(String ngay, String thu, String gioVao, String gioRa, String soGio,
                            boolean muon, boolean veSom, boolean thieuCheckOut) {
}
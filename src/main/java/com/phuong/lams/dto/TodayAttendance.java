package com.phuong.lams.dto;

// Trạng thái chấm công của hôm nay. checkIn, checkOut, ghiChu có thể là null
public record TodayAttendance(String ngay, String thu, String checkIn, String checkOut,
                              boolean coTheCheckIn, boolean coTheCheckOut, String ghiChu) {
}
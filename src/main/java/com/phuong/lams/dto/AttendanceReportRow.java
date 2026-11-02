package com.phuong.lams.dto;

// Một dòng trong báo cáo chấm công tháng của một người
public record AttendanceReportRow(Long userId, String hoTen, String phongBan,
                                  int ngayDiLam, int ngayNghiPhep, int chuaChamCong,
                                  int diMuon, int veSom, int thieuCheckOut,
                                  long phutLamThemThang, String gioLamThemThang,
                                  long phutLamThemNam, String gioLamThemNam,
                                  String canhBao, String canhBaoBadge) {
}
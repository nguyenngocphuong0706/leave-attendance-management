package com.phuong.lams.service;

import com.phuong.lams.dto.AttendanceExportRow;
import com.phuong.lams.dto.AttendanceReport;
import com.phuong.lams.export.AttendanceExcelWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

// Nghiệp vụ xuất báo cáo chấm công ra file Excel
@Service
@RequiredArgsConstructor
public class AttendanceExportService {

    private final AttendanceReportService reportService;
    private final AttendanceExcelWriter excelWriter;

    // Lấy cả hai phần dữ liệu trong MỘT transaction để sheet Tổng hợp và sheet Chi tiết cùng một thời điểm,
    // phạm vi người được xuất do AttendanceReportService quyết định theo vai trò người xuất
    @Transactional(readOnly = true)
    public byte[] xuatExcel(String emailNguoiXuat, YearMonth thang) {
        AttendanceReport baoCao = reportService.layBaoCao(emailNguoiXuat, thang);
        List<AttendanceExportRow> chiTiet = reportService.layChiTietCaNhom(emailNguoiXuat, thang);
        return excelWriter.taoFile(baoCao, chiTiet);
    }
}
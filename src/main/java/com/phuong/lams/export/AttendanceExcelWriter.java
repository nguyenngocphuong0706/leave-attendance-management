package com.phuong.lams.export;

import com.phuong.lams.dto.AttendanceExportRow;
import com.phuong.lams.dto.AttendanceReport;
import com.phuong.lams.dto.AttendanceReportRow;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

// Tạo file Excel (.xlsx) của báo cáo chấm công tháng, gồm hai sheet: Tổng hợp và Chi tiết
@Component
public class AttendanceExcelWriter {

    private static final String[] CAC_COT_TONG_HOP = {
            "Họ tên", "Phòng ban", "Ngày đi làm", "Nghỉ phép", "Chưa chấm công", "Đi muộn", "Về sớm",
            "Thiếu check-out", "Giờ làm thêm trong tháng", "Giờ làm thêm cả năm", "Cảnh báo"};

    private static final String[] CAC_COT_CHI_TIET = {
            "Họ tên", "Ngày", "Thứ", "Giờ vào", "Giờ ra", "Số giờ làm", "Giờ làm thêm được tính", "Ghi chú"};

    public byte[] taoFile(AttendanceReport baoCao, List<AttendanceExportRow> chiTiet) {
        try (Workbook sach = new XSSFWorkbook(); ByteArrayOutputStream luong = new ByteArrayOutputStream()) {
            KieuO kieu = new KieuO(sach);
            veSheetTongHop(sach, kieu, baoCao);
            veSheetChiTiet(sach, kieu, chiTiet);
            sach.write(luong);
            return luong.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được file Excel", e);
        }
    }

    private void veSheetTongHop(Workbook sach, KieuO kieu, AttendanceReport baoCao) {
        Sheet sheet = sach.createSheet("Tổng hợp");

        // Phần đầu: tiêu đề và các dòng giải thích
        taoDongChu(sheet, 0, "BÁO CÁO CHẤM CÔNG THÁNG " + baoCao.thang(), kieu.tieuDe);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, CAC_COT_TONG_HOP.length - 1));
        taoDongChu(sheet, 1, "Phạm vi: " + baoCao.phamVi(), kieu.chu);
        taoDongChu(sheet, 2, "Số ngày làm việc trong tháng: " + baoCao.soNgayLamViec()
                + " (thứ Hai đến thứ Sáu, chưa trừ ngày lễ)", kieu.chu);
        taoDongChu(sheet, 3, "Cột Chưa chấm công chỉ tính ngày làm việc đã qua, không có chấm công và không có đơn nghỉ đã duyệt. "
                + "Hệ thống chưa biết ngày lễ nên cần đối chiếu trước khi kết luận.", kieu.chu);

        // Dòng tiêu đề các cột
        int dongTieuDe = 5;
        taoDongTieuDe(sheet, dongTieuDe, CAC_COT_TONG_HOP, kieu.dauCot);

        // Các dòng dữ liệu. Giờ làm thêm ghi dạng số thập phân để cộng, lọc được trong Excel
        int soDong = dongTieuDe;
        for (AttendanceReportRow r : baoCao.dong()) {
            Row dong = sheet.createRow(++soDong);
            datChu(dong, 0, r.hoTen(), kieu.chu);
            datChu(dong, 1, r.phongBan(), kieu.chu);
            datSo(dong, 2, r.ngayDiLam(), kieu.soNguyen);
            datSo(dong, 3, r.ngayNghiPhep(), kieu.soNguyen);
            datSo(dong, 4, r.chuaChamCong(), kieu.soNguyen);
            datSo(dong, 5, r.diMuon(), kieu.soNguyen);
            datSo(dong, 6, r.veSom(), kieu.soNguyen);
            datSo(dong, 7, r.thieuCheckOut(), kieu.soNguyen);
            datSo(dong, 8, r.phutLamThemThang() / 60.0, kieu.soThapPhan);
            datSo(dong, 9, r.phutLamThemNam() / 60.0, kieu.soThapPhan);
            datChu(dong, 10, r.canhBao() != null ? r.canhBao() : "", kieu.chu);
        }

        hoanThienSheet(sheet, dongTieuDe, soDong, CAC_COT_TONG_HOP.length);
    }

    private void veSheetChiTiet(Workbook sach, KieuO kieu, List<AttendanceExportRow> chiTiet) {
        Sheet sheet = sach.createSheet("Chi tiết");
        int dongTieuDe = 0;
        taoDongTieuDe(sheet, dongTieuDe, CAC_COT_CHI_TIET, kieu.dauCot);

        int soDong = dongTieuDe;
        for (AttendanceExportRow r : chiTiet) {
            Row dong = sheet.createRow(++soDong);
            datChu(dong, 0, r.hoTen(), kieu.chu);
            // Ngày ghi dạng ngày thật của Excel để lọc và sắp xếp được
            Cell oNgay = dong.createCell(1);
            oNgay.setCellValue(Date.from(r.ngay().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            oNgay.setCellStyle(kieu.ngay);
            datChu(dong, 2, r.thu(), kieu.chu);
            datChu(dong, 3, r.gioVao(), kieu.chu);
            datChu(dong, 4, r.gioRa() != null ? r.gioRa() : "", kieu.chu);
            if (r.soGioLam() != null) {
                datSo(dong, 5, r.soGioLam(), kieu.soThapPhan);
            } else {
                dong.createCell(5).setCellStyle(kieu.chu);
            }
            if (r.gioLamThemTinh() != null) {
                datSo(dong, 6, r.gioLamThemTinh(), kieu.soThapPhan);
            } else {
                dong.createCell(6).setCellStyle(kieu.chu);
            }
            datChu(dong, 7, r.ghiChu(), kieu.chu);
        }

        hoanThienSheet(sheet, dongTieuDe, soDong, CAC_COT_CHI_TIET.length);
    }

    // Cố định dòng tiêu đề, thêm bộ lọc và canh độ rộng cột theo nội dung
    private void hoanThienSheet(Sheet sheet, int dongTieuDe, int dongCuoi, int soCot) {
        sheet.createFreezePane(0, dongTieuDe + 1);
        sheet.setAutoFilter(new CellRangeAddress(dongTieuDe, Math.max(dongTieuDe, dongCuoi), 0, soCot - 1));
        for (int i = 0; i < soCot; i++) {
            sheet.autoSizeColumn(i);
            // Chừa thêm một chút lề, và không để cột rộng quá mức cho phép của Excel
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 255 * 256));
        }
    }

    private void taoDongChu(Sheet sheet, int chiSoDong, String noiDung, CellStyle kieu) {
        Row dong = sheet.createRow(chiSoDong);
        datChu(dong, 0, noiDung, kieu);
    }

    private void taoDongTieuDe(Sheet sheet, int chiSoDong, String[] tenCot, CellStyle kieu) {
        Row dong = sheet.createRow(chiSoDong);
        for (int i = 0; i < tenCot.length; i++) {
            datChu(dong, i, tenCot[i], kieu);
        }
    }

    private void datChu(Row dong, int cot, String giaTri, CellStyle kieu) {
        Cell o = dong.createCell(cot);
        o.setCellValue(giaTri);
        o.setCellStyle(kieu);
    }

    private void datSo(Row dong, int cot, double giaTri, CellStyle kieu) {
        Cell o = dong.createCell(cot);
        o.setCellValue(giaTri);
        o.setCellStyle(kieu);
    }

    // Gom các kiểu ô dùng chung, tạo một lần cho cả file (Excel giới hạn số kiểu ô trong một file)
    private static class KieuO {
        final CellStyle tieuDe;
        final CellStyle dauCot;
        final CellStyle chu;
        final CellStyle soNguyen;
        final CellStyle soThapPhan;
        final CellStyle ngay;

        KieuO(Workbook sach) {
            Font chuDam = sach.createFont();
            chuDam.setBold(true);
            Font chuTieuDe = sach.createFont();
            chuTieuDe.setBold(true);
            chuTieuDe.setFontHeightInPoints((short) 14);

            tieuDe = sach.createCellStyle();
            tieuDe.setFont(chuTieuDe);

            dauCot = vien(sach);
            dauCot.setFont(chuDam);
            dauCot.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            dauCot.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dauCot.setAlignment(HorizontalAlignment.CENTER);

            chu = vien(sach);

            soNguyen = vien(sach);
            soNguyen.setDataFormat(sach.createDataFormat().getFormat("0"));

            soThapPhan = vien(sach);
            soThapPhan.setDataFormat(sach.createDataFormat().getFormat("0.00"));

            ngay = vien(sach);
            ngay.setDataFormat(sach.createDataFormat().getFormat("dd/MM/yyyy"));
            ngay.setAlignment(HorizontalAlignment.CENTER);
        }

        private static CellStyle vien(Workbook sach) {
            CellStyle kieu = sach.createCellStyle();
            kieu.setBorderTop(BorderStyle.THIN);
            kieu.setBorderBottom(BorderStyle.THIN);
            kieu.setBorderLeft(BorderStyle.THIN);
            kieu.setBorderRight(BorderStyle.THIN);
            return kieu;
        }
    }
}
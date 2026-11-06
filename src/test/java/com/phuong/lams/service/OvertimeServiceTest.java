package com.phuong.lams.service;

import com.phuong.lams.dto.OvertimeOrderForm;
import com.phuong.lams.entity.OvertimeOrder;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.OvertimeStatus;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.AttendanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.OvertimeOrderRepository;
import com.phuong.lams.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Kiểm tra các luật khi tạo lệnh làm thêm giờ. Mặc định đồng hồ giả đứng ở thứ Hai 02/11/2026 lúc 09:00
class OvertimeServiceTest {

    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate THU_BAY = LocalDate.of(2026, 11, 7);
    private static final LocalDate THU_TU = LocalDate.of(2026, 11, 4);

    private final OvertimeOrderRepository overtimeOrderRepository = mock(OvertimeOrderRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private User quanLy;
    private User nhanVien;
    private User admin;
    private OvertimeService dichVu;

    @BeforeEach
    void chuanBi() {
        dichVu = dichVuLuc(LocalDateTime.of(2026, 11, 2, 9, 0));

        quanLy = nguoiDung(2L, "manager@lams.com", Role.MANAGER, null);
        nhanVien = nguoiDung(1L, "an@lams.com", Role.EMPLOYEE, quanLy);
        admin = nguoiDung(3L, "admin@lams.com", Role.ADMIN, null);

        when(userRepository.findByEmail("manager@lams.com")).thenReturn(Optional.of(quanLy));
        when(userRepository.findByEmail("an@lams.com")).thenReturn(Optional.of(nhanVien));
        when(userRepository.findByEmail("admin@lams.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(nhanVien));
        when(userRepository.findById(2L)).thenReturn(Optional.of(quanLy));
    }

    private OvertimeService dichVuLuc(LocalDateTime thoiDiem) {
        Clock dongHoGia = Clock.fixed(thoiDiem.atZone(MUI_GIO).toInstant(), MUI_GIO);
        return new OvertimeService(overtimeOrderRepository, attendanceRepository,
                leaveRequestRepository, userRepository, dongHoGia);
    }

    private User nguoiDung(Long id, String email, Role vaiTro, User quanLyTrucTiep) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(email);
        user.setRole(vaiTro);
        user.setManager(quanLyTrucTiep);
        return user;
    }

    private OvertimeOrderForm bieuMau(Long nhanVienId, LocalDate ngay, LocalTime tu, LocalTime den) {
        OvertimeOrderForm form = new OvertimeOrderForm();
        form.setEmployeeId(nhanVienId);
        form.setWorkDate(ngay);
        form.setStartTime(tu);
        form.setEndTime(den);
        form.setReason("Hoàn thành đơn hàng gấp");
        return form;
    }

    private OvertimeOrder lenhMuoiHaiGio(LocalDate ngay) {
        OvertimeOrder lenh = new OvertimeOrder();
        lenh.setWorkDate(ngay);
        lenh.setStartTime(LocalTime.of(8, 0));
        lenh.setEndTime(LocalTime.of(20, 0));
        return lenh;
    }

    // Gọi taoLenh và kỳ vọng bị từ chối, trả về lỗi để kiểm tra ô nhập bị gắn lỗi
    private BusinessException biTuChoi(String emailNguoiTao, OvertimeOrderForm form) {
        BusinessException loi = assertThrows(BusinessException.class, () -> dichVu.taoLenh(emailNguoiTao, form));
        verify(overtimeOrderRepository, never()).saveAndFlush(any());
        return loi;
    }

    @Test
    void taoLenh_hopLe_luuLenhOTrangThaiChoPhanHoi() {
        dichVu.taoLenh("manager@lams.com", bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        ArgumentCaptor<OvertimeOrder> bat = ArgumentCaptor.forClass(OvertimeOrder.class);
        verify(overtimeOrderRepository).saveAndFlush(bat.capture());
        assertEquals(OvertimeStatus.PENDING, bat.getValue().getStatus());
        assertSame(nhanVien, bat.getValue().getUser());
        assertSame(quanLy, bat.getValue().getCreatedBy());
    }

    @Test
    void taoLenh_ngayThuong_biTuChoi() {
        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_TU, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("workDate", loi.getField());
    }

    @Test
    void taoLenh_gioKetThucKhongSauGioBatDau_biTuChoi() {
        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(12, 0), LocalTime.of(8, 0)));

        assertEquals("endTime", loi.getField());
    }

    @Test
    void taoLenh_quaMuoiHaiGio_biTuChoi() {
        // 08:00 đến 21:00 là 13 giờ, vượt giới hạn 12 giờ mỗi lệnh
        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(21, 0)));

        assertEquals("endTime", loi.getField());
    }

    @Test
    void taoLenh_dungMuoiHaiGio_duocChapNhan() {
        dichVu.taoLenh("manager@lams.com", bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(20, 0)));

        verify(overtimeOrderRepository).saveAndFlush(any());
    }

    @Test
    void taoLenh_khungGioDaQua_biTuChoi() {
        // Đồng hồ đứng ở thứ Bảy 13:00 trong khi lệnh kết thúc 12:00
        dichVu = dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(13, 0)));

        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("workDate", loi.getField());
    }

    @Test
    void taoLenh_nhanVienKhongCoQuyenTaoLenh_biTuChoi() {
        BusinessException loi = biTuChoi("an@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertNull(loi.getField());
    }

    @Test
    void taoLenh_quanLyKhongPhaiQuanLyTrucTiep_biTuChoi() {
        User quanLyKhac = nguoiDung(4L, "manager2@lams.com", Role.MANAGER, null);
        when(userRepository.findByEmail("manager2@lams.com")).thenReturn(Optional.of(quanLyKhac));

        BusinessException loi = biTuChoi("manager2@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("employeeId", loi.getField());
    }

    @Test
    void taoLenh_tuTaoChoChinhMinh_biTuChoi() {
        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(2L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("employeeId", loi.getField());
    }

    @Test
    void taoLenh_nhanVienKhongCoQuanLy_adminDuocTao() {
        // Người quản lý (id 2) không có quản lý trực tiếp nên ADMIN là người giao việc
        dichVu.taoLenh("admin@lams.com", bieuMau(2L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        verify(overtimeOrderRepository).saveAndFlush(any());
    }

    @Test
    void taoLenh_coDonNghiPhepDaDuyetVaoNgayDo_biTuChoi() {
        when(leaveRequestRepository.demDonTrungNgay(eq(1L), any(), eq(THU_BAY), eq(THU_BAY))).thenReturn(1L);

        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("workDate", loi.getField());
    }

    @Test
    void taoLenh_daCoLenhConHieuLucVaoNgayDo_biTuChoi() {
        when(overtimeOrderRepository.existsByUserIdAndWorkDateAndStatusIn(eq(1L), eq(THU_BAY), any()))
                .thenReturn(true);

        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("workDate", loi.getField());
    }

    @Test
    void taoLenh_vuot40GioTrongThang_biTuChoi() {
        // Đã có ba lệnh 12 giờ trong tháng là 36 giờ, thêm 5 giờ nữa là 41 giờ
        when(overtimeOrderRepository.findByUserIdAndWorkDateBetweenAndStatusIn(eq(1L), any(), any(), any()))
                .thenReturn(List.of(
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 14)),
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 15)),
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 21))));

        BusinessException loi = biTuChoi("manager@lams.com",
                bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(13, 0)));

        assertEquals("workDate", loi.getField());
    }

    @Test
    void taoLenh_dung40GioTrongThang_duocChapNhan() {
        // 36 giờ đã có cộng 4 giờ mới là đúng 40 giờ, chưa vượt giới hạn
        when(overtimeOrderRepository.findByUserIdAndWorkDateBetweenAndStatusIn(eq(1L), any(), any(), any()))
                .thenReturn(List.of(
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 14)),
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 15)),
                        lenhMuoiHaiGio(LocalDate.of(2026, 11, 21))));

        dichVu.taoLenh("manager@lams.com", bieuMau(1L, THU_BAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        verify(overtimeOrderRepository).saveAndFlush(any());
    }
}
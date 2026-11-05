package com.phuong.lams.service;

import com.phuong.lams.config.AttendanceProperties;
import com.phuong.lams.entity.AttendanceRecord;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Kiểm tra luật chấm công với đồng hồ giả: đặt đúng ngày giờ cần thử, không phụ thuộc hôm nay là thứ mấy
class AttendanceServiceTest {

    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate THU_HAI = LocalDate.of(2026, 11, 2);
    private static final LocalDate THU_BAY = LocalDate.of(2026, 11, 7);

    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final OvertimeOrderRepository overtimeOrderRepository = mock(OvertimeOrderRepository.class);
    private final LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private User an;

    @BeforeEach
    void chuanBi() {
        an = new User();
        an.setId(1L);
        an.setEmail("an@lams.com");
        an.setFullName("Nguyễn Văn An");
        an.setRole(Role.EMPLOYEE);
        when(userRepository.findByEmail("an@lams.com")).thenReturn(Optional.of(an));
    }

    // Tạo service với đồng hồ giả đứng yên ở thời điểm cho trước
    private AttendanceService dichVuLuc(LocalDateTime thoiDiem) {
        Clock dongHoGia = Clock.fixed(thoiDiem.atZone(MUI_GIO).toInstant(), MUI_GIO);
        AttendanceProperties props = new AttendanceProperties("08:30", "17:30", 15, "Asia/Ho_Chi_Minh");
        return new AttendanceService(attendanceRepository, overtimeOrderRepository,
                leaveRequestRepository, userRepository, props, dongHoGia);
    }

    private OvertimeOrder lenhLamThem(LocalTime tu, LocalTime den) {
        OvertimeOrder lenh = new OvertimeOrder();
        lenh.setId(5L);
        lenh.setUser(an);
        lenh.setWorkDate(THU_BAY);
        lenh.setStartTime(tu);
        lenh.setEndTime(den);
        when(overtimeOrderRepository.timDeChamCong(eq(1L), eq(THU_BAY), eq(OvertimeStatus.ACCEPTED)))
                .thenReturn(Optional.of(lenh));
        return lenh;
    }

    // Tạo sẵn bản ghi đã check-in để thử check-out
    private AttendanceRecord daCheckIn(LocalDate ngay, LocalTime gioVao, OvertimeOrder lenh) {
        AttendanceRecord banGhi = new AttendanceRecord();
        banGhi.setUser(an);
        banGhi.setWorkDate(ngay);
        banGhi.setCheckIn(LocalDateTime.of(ngay, gioVao));
        banGhi.setOvertimeOrder(lenh);
        when(attendanceRepository.findByUserIdAndWorkDate(1L, ngay)).thenReturn(Optional.of(banGhi));
        return banGhi;
    }

    private AttendanceRecord layBanGhiDaLuu() {
        ArgumentCaptor<AttendanceRecord> bat = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRepository).saveAndFlush(bat.capture());
        return bat.getValue();
    }

    // ---------- Check-in ngày thường ----------

    @Test
    void checkIn_ngayThuongTruocGioDuDi_khongMuon() {
        dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(8, 40))).checkIn("an@lams.com");

        assertFalse(layBanGhiDaLuu().isLate());
    }

    @Test
    void checkIn_dungMocDuDi0845_khongMuon() {
        // Giờ chuẩn 08:30 cộng 15 phút du di, đúng 08:45 vẫn chưa muộn
        dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(8, 45))).checkIn("an@lams.com");

        assertFalse(layBanGhiDaLuu().isLate());
    }

    @Test
    void checkIn_sauMocDuDi0846_diMuon() {
        dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(8, 46))).checkIn("an@lams.com");

        assertTrue(layBanGhiDaLuu().isLate());
    }

    @Test
    void checkIn_dangCoDonNghiPhepDaDuyet_biTuChoi() {
        when(leaveRequestRepository.demDonTrungNgay(eq(1L), any(), eq(THU_HAI), eq(THU_HAI))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(9, 0))).checkIn("an@lams.com"));
        verify(attendanceRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkIn_daCheckInRoi_biTuChoi() {
        daCheckIn(THU_HAI, LocalTime.of(8, 20), null);

        assertThrows(BusinessException.class,
                () -> dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(9, 0))).checkIn("an@lams.com"));
        verify(attendanceRepository, never()).saveAndFlush(any());
    }

    // ---------- Check-in cuối tuần ----------

    @Test
    void checkIn_thuBayKhongCoLenh_biTuChoi() {
        assertThrows(BusinessException.class,
                () -> dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(9, 0))).checkIn("an@lams.com"));
        verify(attendanceRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkIn_thuBayCoLenh_dungGio_ghiNhanLamThemVaKhongMuon() {
        // Lệnh 08:00 đến 12:00, giờ chuẩn của lệnh là 08:00 cộng 15 phút du di
        OvertimeOrder lenh = lenhLamThem(LocalTime.of(8, 0), LocalTime.of(12, 0));

        dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(8, 10))).checkIn("an@lams.com");

        AttendanceRecord banGhi = layBanGhiDaLuu();
        assertSame(lenh, banGhi.getOvertimeOrder());
        assertFalse(banGhi.isLate());
    }

    @Test
    void checkIn_thuBayCoLenh_tinhMuonTheoGioBatDauCuaLenh() {
        // 08:20 chưa muộn nếu theo giờ ngày thường (08:45) nhưng đã muộn theo lệnh bắt đầu 08:00
        lenhLamThem(LocalTime.of(8, 0), LocalTime.of(12, 0));

        dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(8, 20))).checkIn("an@lams.com");

        assertTrue(layBanGhiDaLuu().isLate());
    }

    @Test
    void checkIn_thuBayCoLenh_sauGioKetThucCuaLenh_biTuChoi() {
        lenhLamThem(LocalTime.of(8, 0), LocalTime.of(12, 0));

        assertThrows(BusinessException.class,
                () -> dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(13, 0))).checkIn("an@lams.com"));
        verify(attendanceRepository, never()).saveAndFlush(any());
    }

    // ---------- Check-out ----------

    @Test
    void checkOut_ngayThuongTruoc1730_veSom() {
        AttendanceRecord banGhi = daCheckIn(THU_HAI, LocalTime.of(8, 30), null);

        dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(16, 0))).checkOut("an@lams.com");

        assertNotNull(banGhi.getCheckOut());
        assertTrue(banGhi.isLeftEarly());
    }

    @Test
    void checkOut_ngayThuongDung1730_khongVeSom() {
        AttendanceRecord banGhi = daCheckIn(THU_HAI, LocalTime.of(8, 30), null);

        dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(17, 30))).checkOut("an@lams.com");

        assertFalse(banGhi.isLeftEarly());
    }

    @Test
    void checkOut_ngayLamThem_tinhVeSomTheoGioKetThucCuaLenh() {
        OvertimeOrder lenh = lenhLamThem(LocalTime.of(8, 0), LocalTime.of(12, 0));
        AttendanceRecord banGhi = daCheckIn(THU_BAY, LocalTime.of(8, 5), lenh);

        // 11:00 là trước 12:00 của lệnh nên về sớm, dù chưa tới giờ 17:30 ngày thường
        dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(11, 0))).checkOut("an@lams.com");

        assertTrue(banGhi.isLeftEarly());
    }

    @Test
    void checkOut_ngayLamThem_raSauGioKetThucCuaLenh_khongVeSom() {
        OvertimeOrder lenh = lenhLamThem(LocalTime.of(8, 0), LocalTime.of(12, 0));
        AttendanceRecord banGhi = daCheckIn(THU_BAY, LocalTime.of(8, 5), lenh);

        // 12:05 đã qua 12:00 của lệnh nên không về sớm, dù trước 17:30
        dichVuLuc(LocalDateTime.of(THU_BAY, LocalTime.of(12, 5))).checkOut("an@lams.com");

        assertFalse(banGhi.isLeftEarly());
    }

    @Test
    void checkOut_chuaCheckIn_biTuChoi() {
        BusinessException loi = assertThrows(BusinessException.class,
                () -> dichVuLuc(LocalDateTime.of(THU_HAI, LocalTime.of(17, 0))).checkOut("an@lams.com"));

        assertEquals("Hôm nay bạn chưa check-in", loi.getMessage());
    }
}
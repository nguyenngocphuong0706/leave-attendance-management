package com.phuong.lams.service;

import com.phuong.lams.dto.LeaveRequestForm;
import com.phuong.lams.entity.LeaveBalance;
import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.entity.LeaveType;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.Applicability;
import com.phuong.lams.enums.Gender;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.LeaveBalanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.LeaveTypeRepository;
import com.phuong.lams.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Kiểm tra các luật khi nhân viên xin nghỉ phép. Đồng hồ giả đứng ở thứ Hai 02/11/2026
class LeaveRequestServiceTest {

    private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
    private final LeaveBalanceRepository leaveBalanceRepository = mock(LeaveBalanceRepository.class);
    private final LeaveTypeRepository leaveTypeRepository = mock(LeaveTypeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private User an;
    private LeaveType nghiPhepNam;
    private LeaveBalance soDu;
    private LeaveRequestService dichVu;

    @BeforeEach
    void chuanBi() {
        Clock dongHoGia = Clock.fixed(
                LocalDateTime.of(2026, 11, 2, 9, 0).atZone(MUI_GIO).toInstant(), MUI_GIO);
        dichVu = new LeaveRequestService(leaveRequestRepository, leaveBalanceRepository,
                leaveTypeRepository, userRepository, dongHoGia);

        an = new User();
        an.setId(1L);
        an.setEmail("an@lams.com");
        an.setRole(Role.EMPLOYEE);
        an.setGender(Gender.MALE);
        when(userRepository.findByEmail("an@lams.com")).thenReturn(Optional.of(an));

        nghiPhepNam = new LeaveType();
        nghiPhepNam.setId(10L);
        nghiPhepNam.setName("Nghỉ phép năm");
        nghiPhepNam.setDaysPerYear(12);
        nghiPhepNam.setAppliesTo(Applicability.ALL);
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.of(nghiPhepNam));

        soDu = new LeaveBalance();
        soDu.setUser(an);
        soDu.setLeaveType(nghiPhepNam);
        soDu.setYear(2026);
        soDu.setTotalDays(12);
        soDu.setUsedDays(0);
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 10L, 2026)).thenReturn(Optional.of(soDu));
    }

    private LeaveRequestForm donXin(LocalDate tu, LocalDate den) {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setLeaveTypeId(10L);
        form.setStartDate(tu);
        form.setEndDate(den);
        form.setReason("Việc gia đình");
        return form;
    }

    // Gọi taoDon và kỳ vọng bị từ chối, trả về lỗi để kiểm tra ô nhập bị gắn lỗi
    private BusinessException biTuChoi(LeaveRequestForm form) {
        BusinessException loi = assertThrows(BusinessException.class, () -> dichVu.taoDon("an@lams.com", form));
        verify(leaveRequestRepository, never()).save(any());
        return loi;
    }

    @Test
    void taoDon_hopLe_luuDonVoiSoNgayLamViecDungVaTrangThaiChoDuyet() {
        // Thứ Hai đến thứ Tư là 3 ngày làm việc
        dichVu.taoDon("an@lams.com", donXin(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)));

        ArgumentCaptor<LeaveRequest> bat = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(bat.capture());
        assertEquals(3, bat.getValue().getDays());
        assertEquals(LeaveStatus.PENDING, bat.getValue().getStatus());
    }

    @Test
    void taoDon_bacQuaCuoiTuan_khongTinhThuBayVaChuNhat() {
        // Thứ Sáu 06/11 đến thứ Hai 09/11: chỉ có thứ Sáu và thứ Hai, tức 2 ngày
        dichVu.taoDon("an@lams.com", donXin(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9)));

        ArgumentCaptor<LeaveRequest> bat = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(bat.capture());
        assertEquals(2, bat.getValue().getDays());
    }

    @Test
    void taoDon_chiGomThuBayVaChuNhat_biTuChoi() {
        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)));

        assertEquals("startDate", loi.getField());
    }

    @Test
    void taoDon_ngayBatDauOQuaKhu_biTuChoi() {
        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 30)));

        assertEquals("startDate", loi.getField());
    }

    @Test
    void taoDon_ngayKetThucTruocNgayBatDau_biTuChoi() {
        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 3)));

        assertEquals("endDate", loi.getField());
    }

    @Test
    void taoDon_keoDaiSangNamKhac_biTuChoi() {
        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 4)));

        assertEquals("endDate", loi.getField());
    }

    @Test
    void taoDon_trungVoiDonDaCo_biTuChoi() {
        when(leaveRequestRepository.demDonTrungNgay(eq(1L), any(), any(), any())).thenReturn(1L);

        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)));

        assertEquals("startDate", loi.getField());
    }

    @Test
    void taoDon_khongDuNgayPhep_biTuChoi() {
        soDu.setTotalDays(2);

        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4)));

        assertEquals("leaveTypeId", loi.getField());
        assertTrue(loi.getMessage().contains("Không đủ ngày phép"));
    }

    @Test
    void taoDon_cacNgayDangChoDuyetPhaiDuocTruRaKhiKiemTraDuNgay() {
        // Còn 3 ngày nhưng đã có đơn chờ duyệt 3 ngày, nên xin thêm 1 ngày là không đủ
        soDu.setTotalDays(3);
        when(leaveRequestRepository.tongNgayTheoTrangThai(
                eq(1L), eq(10L), eq(LeaveStatus.PENDING), any(), any())).thenReturn(3L);

        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12)));

        assertEquals("leaveTypeId", loi.getField());
    }

    @Test
    void taoDon_loaiNghiChiDanhChoNuNhungNhanVienLaNam_biTuChoi() {
        nghiPhepNam.setAppliesTo(Applicability.FEMALE);

        BusinessException loi = biTuChoi(donXin(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2)));

        assertEquals("leaveTypeId", loi.getField());
    }
}
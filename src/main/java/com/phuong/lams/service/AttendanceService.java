package com.phuong.lams.service;

import com.phuong.lams.config.AttendanceProperties;
import com.phuong.lams.dto.AttendanceRow;
import com.phuong.lams.dto.TodayAttendance;
import com.phuong.lams.entity.AttendanceRecord;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.AttendanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// Nghiệp vụ chấm công của nhân viên
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final AttendanceProperties props;
    private final Clock clock;

    // Tháng hiện tại theo giờ công ty
    public YearMonth thangHienTai() {
        return YearMonth.now(clock);
    }

    // Trạng thái chấm công của hôm nay: đã vào, đã ra chưa, có được bấm nút nào không
    @Transactional(readOnly = true)
    public TodayAttendance layHomNay(String email) {
        User user = timNguoiDung(email);
        LocalDate homNay = LocalDate.now(clock);
        Optional<AttendanceRecord> banGhi = attendanceRepository.findByUserIdAndWorkDate(user.getId(), homNay);
        Optional<String> lyDo = lyDoKhongChamCong(user, homNay);

        String gioVao = banGhi.map(b -> b.getCheckIn().format(GIO)).orElse(null);
        String gioRa = banGhi.map(AttendanceRecord::getCheckOut).map(t -> t.format(GIO)).orElse(null);
        boolean coTheCheckIn = banGhi.isEmpty() && lyDo.isEmpty();
        boolean coTheCheckOut = banGhi.isPresent() && banGhi.get().getCheckOut() == null;
        // Chỉ báo lý do khi hôm nay chưa chấm công và không thể chấm công
        String ghiChu = banGhi.isEmpty() ? lyDo.orElse(null) : null;

        return new TodayAttendance(homNay.format(NGAY), tenThu(homNay.getDayOfWeek()),
                gioVao, gioRa, coTheCheckIn, coTheCheckOut, ghiChu);
    }

    // Lịch sử chấm công của một tháng, ngày mới nhất lên đầu
    @Transactional(readOnly = true)
    public List<AttendanceRow> layLichSu(String email, YearMonth thang) {
        LocalDate homNay = LocalDate.now(clock);
        return attendanceRepository.layTheoKhoang(email, thang.atDay(1), thang.atEndOfMonth()).stream()
                .map(b -> chuyenThanhDong(b, homNay))
                .toList();
    }

    @Transactional
    public void checkIn(String email) {
        User user = timNguoiDung(email);
        LocalDateTime bayGio = LocalDateTime.now(clock);
        LocalDate homNay = bayGio.toLocalDate();

        lyDoKhongChamCong(user, homNay).ifPresent(lyDo -> {
            throw new BusinessException(lyDo);
        });
        attendanceRepository.findByUserIdAndWorkDate(user.getId(), homNay).ifPresent(b -> {
            throw new BusinessException("Hôm nay bạn đã check-in lúc " + b.getCheckIn().format(GIO));
        });

        AttendanceRecord banGhi = new AttendanceRecord();
        banGhi.setUser(user);
        banGhi.setWorkDate(homNay);
        banGhi.setCheckIn(bayGio);
        // Muộn khi vào sau giờ chuẩn cộng thời gian du di
        banGhi.setLate(bayGio.toLocalTime().isAfter(
                props.gioVaoChuan().plusMinutes(props.lateGraceMinutes())));
        try {
            // Ghi xuống database ngay để ràng buộc UNIQUE phát huy tác dụng
            attendanceRepository.saveAndFlush(banGhi);
        } catch (DataIntegrityViolationException e) {
            // Hai yêu cầu cùng lúc (bấm đúp): người đến sau bị database chặn
            throw new BusinessException("Hôm nay bạn đã check-in rồi");
        }
    }

    @Transactional
    public void checkOut(String email) {
        User user = timNguoiDung(email);
        LocalDateTime bayGio = LocalDateTime.now(clock);

        AttendanceRecord banGhi = attendanceRepository
                .findByUserIdAndWorkDate(user.getId(), bayGio.toLocalDate())
                .orElseThrow(() -> new BusinessException("Hôm nay bạn chưa check-in"));
        if (banGhi.getCheckOut() != null) {
            throw new BusinessException("Hôm nay bạn đã check-out lúc " + banGhi.getCheckOut().format(GIO));
        }

        banGhi.setCheckOut(bayGio);
        // Về sớm khi ra trước giờ chuẩn. Không cần save(): dirty checking tự cập nhật
        banGhi.setLeftEarly(bayGio.toLocalTime().isBefore(props.gioRaChuan()));
    }

    // Lý do ngày này không được chấm công, trống nếu được chấm công
    private Optional<String> lyDoKhongChamCong(User user, LocalDate ngay) {
        DayOfWeek thu = ngay.getDayOfWeek();
        if (thu == DayOfWeek.SATURDAY || thu == DayOfWeek.SUNDAY) {
            return Optional.of("Hôm nay là ngày nghỉ cuối tuần, không thể chấm công");
        }
        long soDonNghi = leaveRequestRepository.demDonTrungNgay(
                user.getId(), List.of(LeaveStatus.APPROVED), ngay, ngay);
        if (soDonNghi > 0) {
            return Optional.of("Hôm nay bạn có đơn nghỉ phép đã được duyệt, không thể chấm công");
        }
        return Optional.empty();
    }

    private AttendanceRow chuyenThanhDong(AttendanceRecord banGhi, LocalDate homNay) {
        LocalDateTime ra = banGhi.getCheckOut();
        return new AttendanceRow(
                banGhi.getWorkDate().format(NGAY),
                tenThu(banGhi.getWorkDate().getDayOfWeek()),
                banGhi.getCheckIn().format(GIO),
                ra != null ? ra.format(GIO) : "-",
                ra != null ? dinhDangSoGio(Duration.between(banGhi.getCheckIn(), ra)) : "-",
                banGhi.isLate(),
                banGhi.isLeftEarly(),
                // Thiếu check-out chỉ tính cho ngày đã qua, hôm nay vẫn còn kịp
                ra == null && banGhi.getWorkDate().isBefore(homNay));
    }

    private String dinhDangSoGio(Duration khoang) {
        long phut = khoang.toMinutes();
        return (phut / 60) + " giờ " + String.format("%02d", phut % 60) + " phút";
    }

    private String tenThu(DayOfWeek thu) {
        return switch (thu) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private User timNguoiDung(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
    }
}
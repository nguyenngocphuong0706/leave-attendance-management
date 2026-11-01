package com.phuong.lams.service;

import com.phuong.lams.config.AttendanceProperties;
import com.phuong.lams.dto.AttendanceRow;
import com.phuong.lams.dto.TodayAttendance;
import com.phuong.lams.entity.AttendanceRecord;
import com.phuong.lams.entity.OvertimeOrder;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.enums.OvertimeStatus;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.AttendanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.OvertimeOrderRepository;
import com.phuong.lams.repository.UserRepository;
import com.phuong.lams.util.ThoiGianUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final OvertimeOrderRepository overtimeOrderRepository;
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
        LocalDateTime bayGio = LocalDateTime.now(clock);
        LocalDate homNay = bayGio.toLocalDate();
        Optional<AttendanceRecord> banGhi = attendanceRepository.findByUserIdAndWorkDate(user.getId(), homNay);
        // Cuối tuần thì tìm lệnh làm thêm đã đồng ý cho hôm nay
        Optional<OvertimeOrder> lenh = ThoiGianUtil.laCuoiTuan(homNay)
                ? overtimeOrderRepository.findByUserIdAndWorkDateAndStatus(user.getId(), homNay, OvertimeStatus.ACCEPTED)
                : Optional.empty();
        Optional<String> lyDo = lyDoKhongChamCong(user, homNay, lenh, bayGio.toLocalTime());

        String gioVao = banGhi.map(b -> b.getCheckIn().format(GIO)).orElse(null);
        String gioRa = banGhi.map(AttendanceRecord::getCheckOut).map(t -> t.format(GIO)).orElse(null);
        boolean coTheCheckIn = banGhi.isEmpty() && lyDo.isEmpty();
        boolean coTheCheckOut = banGhi.isPresent() && banGhi.get().getCheckOut() == null;
        // Chỉ báo lý do khi hôm nay chưa chấm công và không thể chấm công
        String ghiChu = banGhi.isEmpty() ? lyDo.orElse(null) : null;
        String lamThem = lenh.map(o -> "Làm thêm theo lệnh: " + o.getStartTime().format(GIO)
                + " đến " + o.getEndTime().format(GIO)).orElse(null);

        return new TodayAttendance(homNay.format(NGAY), ThoiGianUtil.tenThu(homNay.getDayOfWeek()),
                gioVao, gioRa, coTheCheckIn, coTheCheckOut, ghiChu, lamThem);
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

        // Lấy lệnh có khóa: nếu quản lý đang hủy đúng lệnh này thì chờ họ xong rồi mới thấy kết quả
        Optional<OvertimeOrder> lenh = ThoiGianUtil.laCuoiTuan(homNay)
                ? overtimeOrderRepository.timDeChamCong(user.getId(), homNay, OvertimeStatus.ACCEPTED)
                : Optional.empty();

        lyDoKhongChamCong(user, homNay, lenh, bayGio.toLocalTime()).ifPresent(lyDo -> {
            throw new BusinessException(lyDo);
        });
        attendanceRepository.findByUserIdAndWorkDate(user.getId(), homNay).ifPresent(b -> {
            throw new BusinessException("Hôm nay bạn đã check-in lúc " + b.getCheckIn().format(GIO));
        });

        AttendanceRecord banGhi = new AttendanceRecord();
        banGhi.setUser(user);
        banGhi.setWorkDate(homNay);
        banGhi.setCheckIn(bayGio);
        // Giờ vào chuẩn: ngày làm thêm lấy theo lệnh, ngày thường lấy theo cấu hình
        LocalTime gioVaoChuan = lenh.map(OvertimeOrder::getStartTime).orElse(props.gioVaoChuan());
        banGhi.setLate(bayGio.toLocalTime().isAfter(gioVaoChuan.plusMinutes(props.lateGraceMinutes())));
        lenh.ifPresent(banGhi::setOvertimeOrder);
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
        // Giờ ra chuẩn: ngày làm thêm lấy theo lệnh, ngày thường lấy theo cấu hình
        LocalTime gioRaChuan = banGhi.getOvertimeOrder() != null
                ? banGhi.getOvertimeOrder().getEndTime()
                : props.gioRaChuan();
        // Không cần save(): dirty checking tự cập nhật
        banGhi.setLeftEarly(bayGio.toLocalTime().isBefore(gioRaChuan));
    }

    // Lý do hôm nay không được chấm công, trống nếu được chấm công
    private Optional<String> lyDoKhongChamCong(User user, LocalDate ngay, Optional<OvertimeOrder> lenh, LocalTime gio) {
        long soDonNghi = leaveRequestRepository.demDonTrungNgay(
                user.getId(), List.of(LeaveStatus.APPROVED), ngay, ngay);
        if (soDonNghi > 0) {
            return Optional.of("Hôm nay bạn có đơn nghỉ phép đã được duyệt, không thể chấm công");
        }
        if (ThoiGianUtil.laCuoiTuan(ngay)) {
            if (lenh.isEmpty()) {
                return Optional.of("Hôm nay là ngày nghỉ cuối tuần và bạn chưa có lệnh làm thêm được đồng ý");
            }
            if (gio.isAfter(lenh.get().getEndTime())) {
                return Optional.of("Khung giờ làm thêm hôm nay đã kết thúc lúc " + lenh.get().getEndTime().format(GIO));
            }
        }
        return Optional.empty();
    }

    private AttendanceRow chuyenThanhDong(AttendanceRecord banGhi, LocalDate homNay) {
        LocalDateTime ra = banGhi.getCheckOut();
        OvertimeOrder lenh = banGhi.getOvertimeOrder();
        return new AttendanceRow(
                banGhi.getWorkDate().format(NGAY),
                ThoiGianUtil.tenThu(banGhi.getWorkDate().getDayOfWeek()),
                banGhi.getCheckIn().format(GIO),
                ra != null ? ra.format(GIO) : "-",
                ra != null ? ThoiGianUtil.dinhDangSoGio(Duration.between(banGhi.getCheckIn(), ra)) : "-",
                banGhi.isLate(),
                banGhi.isLeftEarly(),
                // Thiếu check-out chỉ tính cho ngày đã qua, hôm nay vẫn còn kịp
                ra == null && banGhi.getWorkDate().isBefore(homNay),
                lenh != null,
                // Giờ làm thêm được tính: phần nằm trong khung giờ của lệnh
                lenh != null && ra != null
                        ? ThoiGianUtil.dinhDangSoGio(lenh.thoiGianTrongKhung(banGhi.getCheckIn(), ra))
                        : null);
    }

    private User timNguoiDung(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
    }
}
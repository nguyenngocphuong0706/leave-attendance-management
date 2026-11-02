package com.phuong.lams.service;

import com.phuong.lams.dto.AttendanceDetail;
import com.phuong.lams.dto.AttendanceReport;
import com.phuong.lams.dto.AttendanceReportRow;
import com.phuong.lams.entity.AttendanceRecord;
import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.AttendanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.UserRepository;
import com.phuong.lams.util.ThoiGianUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Nghiệp vụ báo cáo chấm công tháng theo vai trò
@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    // Giới hạn làm thêm thông thường trong một năm và ngưỡng bắt đầu cảnh báo
    private static final Duration GIOI_HAN_NAM = Duration.ofHours(200);
    private static final Duration NGUONG_CANH_BAO_NAM = Duration.ofHours(180);
    private static final DateTimeFormatter THANG = DateTimeFormatter.ofPattern("MM/yyyy");

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceService attendanceService;
    private final Clock clock;

    // Báo cáo của tất cả những người trong phạm vi của người xem
    @Transactional(readOnly = true)
    public AttendanceReport layBaoCao(String emailNguoiXem, YearMonth thang) {
        User nguoiXem = timNguoiDung(emailNguoiXem);
        List<User> nguoiTrongPhamVi = layNguoiTrongPhamVi(nguoiXem);
        List<Long> ids = nguoiTrongPhamVi.stream().map(User::getId).toList();
        LocalDate homNay = LocalDate.now(clock);
        LocalDate dauNam = LocalDate.of(thang.getYear(), 1, 1);
        LocalDate cuoiNam = LocalDate.of(thang.getYear(), 12, 31);

        // Lấy dữ liệu của CẢ NHÓM bằng ba câu truy vấn, rồi chia theo từng người trong bộ nhớ.
        // Cách này tránh việc hỏi database ba lần cho mỗi nhân viên (vấn đề N+1)
        Map<Long, List<AttendanceRecord>> banGhiThang = attendanceRepository
                .layTheoNguoiVaKhoang(ids, thang.atDay(1), thang.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));
        Map<Long, Duration> lamThemNam = attendanceRepository
                .layLamThemTheoNguoiVaKhoang(ids, dauNam, cuoiNam).stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId(),
                        Collectors.reducing(Duration.ZERO, this::thoiGianLamThem, Duration::plus)));
        Map<Long, List<LeaveRequest>> donNghi = leaveRequestRepository
                .layTheoNguoiTrangThaiVaKhoang(ids, LeaveStatus.APPROVED, thang.atDay(1), thang.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId()));

        List<AttendanceReportRow> dong = nguoiTrongPhamVi.stream()
                .map(u -> tinhHang(u,
                        banGhiThang.getOrDefault(u.getId(), List.of()),
                        donNghi.getOrDefault(u.getId(), List.of()),
                        lamThemNam.getOrDefault(u.getId(), Duration.ZERO),
                        thang, homNay))
                .toList();

        return new AttendanceReport(thang.format(THANG), moTaPhamVi(nguoiXem), demNgayLamViec(thang), dong);
    }

    // Chi tiết từng ngày của một người, chỉ khi người xem có quyền
    @Transactional(readOnly = true)
    public AttendanceDetail layChiTiet(String emailNguoiXem, Long userId, YearMonth thang) {
        User nguoiXem = timNguoiDung(emailNguoiXem);
        // Người không tồn tại và người không được phép xem cùng báo một thông điệp, để không lộ thông tin
        User mucTieu = userRepository.findById(userId)
                .filter(u -> duocXem(nguoiXem, u))
                .orElseThrow(() -> new BusinessException("Không tìm thấy hoặc bạn không có quyền xem chấm công của người này"));
        return new AttendanceDetail(
                mucTieu.getFullName(),
                mucTieu.getDepartment() != null ? mucTieu.getDepartment().getName() : "Chưa có",
                thang.format(THANG),
                attendanceService.layLichSu(mucTieu.getEmail(), thang));
    }

    // Phạm vi người xem theo vai trò
    private List<User> layNguoiTrongPhamVi(User nguoiXem) {
        return switch (nguoiXem.getRole()) {
            case ADMIN -> userRepository.layTatCaDangHoatDong();
            case MANAGER -> userRepository.layNhomCuaQuanLy(nguoiXem.getId());
            case EMPLOYEE -> List.of(nguoiXem);
        };
    }

    private String moTaPhamVi(User nguoiXem) {
        return switch (nguoiXem.getRole()) {
            case ADMIN -> "Toàn bộ người dùng đang hoạt động";
            case MANAGER -> "Bạn và các nhân viên bạn quản lý trực tiếp";
            case EMPLOYEE -> "Chỉ của bạn";
        };
    }

    // Quy tắc ai được xem chấm công của ai: chính mình, quản lý trực tiếp, hoặc ADMIN
    private boolean duocXem(User nguoiXem, User mucTieu) {
        if (nguoiXem.getId().equals(mucTieu.getId()) || nguoiXem.getRole() == Role.ADMIN) {
            return true;
        }
        return nguoiXem.getRole() == Role.MANAGER
                && mucTieu.getManager() != null
                && mucTieu.getManager().getId().equals(nguoiXem.getId());
    }

    private AttendanceReportRow tinhHang(User user, List<AttendanceRecord> banGhi, List<LeaveRequest> donNghi,
                                         Duration lamThemNam, YearMonth thang, LocalDate homNay) {
        Set<LocalDate> ngayCoChamCong = banGhi.stream()
                .map(AttendanceRecord::getWorkDate)
                .collect(Collectors.toSet());
        Set<LocalDate> ngayNghiPhep = layNgayNghiPhep(donNghi, thang);

        // Ngày đi làm chỉ tính ngày thường, ngày làm thêm cuối tuần có lệnh nên tính riêng
        int ngayDiLam = (int) banGhi.stream().filter(b -> b.getOvertimeOrder() == null).count();
        int diMuon = (int) banGhi.stream().filter(AttendanceRecord::isLate).count();
        int veSom = (int) banGhi.stream().filter(AttendanceRecord::isLeftEarly).count();
        int thieuCheckOut = (int) banGhi.stream()
                .filter(b -> b.getCheckOut() == null && b.getWorkDate().isBefore(homNay))
                .count();
        Duration lamThemThang = banGhi.stream()
                .map(this::thoiGianLamThem)
                .reduce(Duration.ZERO, Duration::plus);

        String canhBao = null;
        String canhBaoBadge = null;
        if (lamThemNam.compareTo(GIOI_HAN_NAM) > 0) {
            canhBao = "Vượt 200 giờ làm thêm trong năm";
            canhBaoBadge = "danger";
        } else if (lamThemNam.compareTo(NGUONG_CANH_BAO_NAM) >= 0) {
            canhBao = "Gần chạm 200 giờ làm thêm trong năm";
            canhBaoBadge = "warning";
        }

        return new AttendanceReportRow(
                user.getId(),
                user.getFullName(),
                user.getDepartment() != null ? user.getDepartment().getName() : "Chưa có",
                ngayDiLam,
                ngayNghiPhep.size(),
                demNgayChuaChamCong(thang, homNay, ngayCoChamCong, ngayNghiPhep),
                diMuon,
                veSom,
                thieuCheckOut,
                lamThemThang.toMinutes(),
                ThoiGianUtil.dinhDangSoGio(lamThemThang),
                lamThemNam.toMinutes(),
                ThoiGianUtil.dinhDangSoGio(lamThemNam),
                canhBao,
                canhBaoBadge);
    }

    // Các ngày làm việc trong tháng thuộc những đơn nghỉ đã duyệt (đơn có thể kéo dài qua nhiều tháng)
    private Set<LocalDate> layNgayNghiPhep(List<LeaveRequest> donNghi, YearMonth thang) {
        Set<LocalDate> ngay = new HashSet<>();
        for (LeaveRequest don : donNghi) {
            LocalDate tu = don.getStartDate().isAfter(thang.atDay(1)) ? don.getStartDate() : thang.atDay(1);
            LocalDate den = don.getEndDate().isBefore(thang.atEndOfMonth()) ? don.getEndDate() : thang.atEndOfMonth();
            for (LocalDate d = tu; !d.isAfter(den); d = d.plusDays(1)) {
                if (!ThoiGianUtil.laCuoiTuan(d)) {
                    ngay.add(d);
                }
            }
        }
        return ngay;
    }

    // Số ngày làm việc ĐÃ QUA trong tháng mà không có chấm công và không có đơn nghỉ đã duyệt
    private int demNgayChuaChamCong(YearMonth thang, LocalDate homNay,
                                    Set<LocalDate> ngayCoChamCong, Set<LocalDate> ngayNghiPhep) {
        LocalDate homQua = homNay.minusDays(1);
        LocalDate cuoi = thang.atEndOfMonth().isBefore(homQua) ? thang.atEndOfMonth() : homQua;
        int dem = 0;
        for (LocalDate d = thang.atDay(1); !d.isAfter(cuoi); d = d.plusDays(1)) {
            if (!ThoiGianUtil.laCuoiTuan(d) && !ngayCoChamCong.contains(d) && !ngayNghiPhep.contains(d)) {
                dem++;
            }
        }
        return dem;
    }

    private int demNgayLamViec(YearMonth thang) {
        int dem = 0;
        for (LocalDate d = thang.atDay(1); !d.isAfter(thang.atEndOfMonth()); d = d.plusDays(1)) {
            if (!ThoiGianUtil.laCuoiTuan(d)) {
                dem++;
            }
        }
        return dem;
    }

    // Giờ làm thêm của một bản ghi: chỉ phần nằm trong khung giờ của lệnh, và phải đã check-out
    private Duration thoiGianLamThem(AttendanceRecord banGhi) {
        if (banGhi.getOvertimeOrder() == null || banGhi.getCheckOut() == null) {
            return Duration.ZERO;
        }
        return banGhi.getOvertimeOrder().thoiGianTrongKhung(banGhi.getCheckIn(), banGhi.getCheckOut());
    }

    private User timNguoiDung(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
    }
}
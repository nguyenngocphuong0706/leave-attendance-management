package com.phuong.lams.service;

import com.phuong.lams.dto.OvertimeOrderForm;
import com.phuong.lams.dto.OvertimeRow;
import com.phuong.lams.dto.UserOption;
import com.phuong.lams.entity.OvertimeOrder;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.enums.OvertimeStatus;
import com.phuong.lams.enums.Role;
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
import java.util.ArrayList;
import java.util.List;

// Nghiệp vụ lệnh làm thêm giờ: quản lý tạo và hủy, nhân viên đồng ý hoặc từ chối
@Service
@RequiredArgsConstructor
public class OvertimeService {

    private static final Duration GIOI_HAN_MOT_LENH = Duration.ofHours(12);
    private static final Duration GIOI_HAN_MOT_THANG = Duration.ofHours(40);
    private static final int GHI_CHU_TOI_DA = 500;
    // Lệnh còn hiệu lực là lệnh đang chờ phản hồi hoặc đã được đồng ý
    private static final List<OvertimeStatus> CON_HIEU_LUC = List.of(OvertimeStatus.PENDING, OvertimeStatus.ACCEPTED);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final OvertimeOrderRepository overtimeOrderRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // Các lệnh giao cho tôi
    @Transactional(readOnly = true)
    public List<OvertimeRow> layCuaToi(String email) {
        LocalDateTime bayGio = LocalDateTime.now(clock);
        return overtimeOrderRepository.layCuaNhanVien(email).stream()
                .map(o -> chuyenThanhDong(o, bayGio))
                .toList();
    }

    // Các lệnh do tôi tạo
    @Transactional(readOnly = true)
    public List<OvertimeRow> layDaTao(String email) {
        LocalDateTime bayGio = LocalDateTime.now(clock);
        return overtimeOrderRepository.layDoNguoiTao(email).stream()
                .map(o -> chuyenThanhDong(o, bayGio))
                .toList();
    }

    // Những nhân viên mà người này được giao làm thêm: người mình quản lý trực tiếp,
    // và nếu là ADMIN thì thêm những người chưa có quản lý
    @Transactional(readOnly = true)
    public List<UserOption> layNhanVienDuocGiao(String email) {
        User nguoiTao = timNguoiDung(email);
        if (nguoiTao.getRole() == Role.EMPLOYEE) {
            return List.of();
        }
        List<User> danhSach = new ArrayList<>(
                userRepository.findByManagerIdAndActiveTrueOrderByFullNameAsc(nguoiTao.getId()));
        if (nguoiTao.getRole() == Role.ADMIN) {
            danhSach.addAll(userRepository.findByManagerIsNullAndActiveTrueAndIdNotOrderByFullNameAsc(nguoiTao.getId()));
        }
        return danhSach.stream()
                .map(u -> new UserOption(u.getId(), u.getFullName() + " (" + u.getEmail() + ")"))
                .toList();
    }

    @Transactional
    public void taoLenh(String emailNguoiTao, OvertimeOrderForm form) {
        User nguoiTao = timNguoiDung(emailNguoiTao);
        if (!nguoiTao.isActive() || nguoiTao.getRole() == Role.EMPLOYEE) {
            throw new BusinessException("Bạn không có quyền tạo lệnh làm thêm");
        }
        User nhanVien = userRepository.findById(form.getEmployeeId())
                .orElseThrow(() -> new BusinessException("employeeId", "Nhân viên không tồn tại"));
        kiemTraQuyenTaoLenh(nguoiTao, nhanVien);

        LocalDate ngay = form.getWorkDate();
        LocalTime tu = form.getStartTime();
        LocalTime den = form.getEndTime();

        // Luật về ngày và giờ
        if (!ThoiGianUtil.laCuoiTuan(ngay)) {
            throw new BusinessException("workDate", "Chỉ tạo lệnh làm thêm cho thứ Bảy hoặc Chủ nhật");
        }
        if (!den.isAfter(tu)) {
            throw new BusinessException("endTime", "Giờ kết thúc phải sau giờ bắt đầu");
        }
        Duration thoiLuong = Duration.between(tu, den);
        if (thoiLuong.compareTo(GIOI_HAN_MOT_LENH) > 0) {
            throw new BusinessException("endTime", "Một lệnh làm thêm tối đa 12 giờ");
        }
        if (LocalDateTime.of(ngay, den).isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException("workDate", "Khung giờ làm thêm đã qua, hãy chọn ngày giờ trong tương lai");
        }

        // Không tạo lệnh vào ngày nhân viên đã có đơn nghỉ phép được duyệt
        long soDonNghi = leaveRequestRepository.demDonTrungNgay(
                nhanVien.getId(), List.of(LeaveStatus.APPROVED), ngay, ngay);
        if (soDonNghi > 0) {
            throw new BusinessException("workDate", "Nhân viên có đơn nghỉ phép đã duyệt vào ngày này");
        }

        // Mỗi ngày chỉ một lệnh còn hiệu lực
        if (overtimeOrderRepository.existsByUserIdAndWorkDateAndStatusIn(nhanVien.getId(), ngay, CON_HIEU_LUC)) {
            throw new BusinessException("workDate", "Nhân viên đã có lệnh làm thêm còn hiệu lực vào ngày này");
        }

        // Giới hạn 40 giờ mỗi tháng, tính cả các lệnh còn hiệu lực đã có
        YearMonth thang = YearMonth.from(ngay);
        Duration daCo = overtimeOrderRepository
                .findByUserIdAndWorkDateBetweenAndStatusIn(nhanVien.getId(), thang.atDay(1), thang.atEndOfMonth(), CON_HIEU_LUC)
                .stream()
                .map(o -> Duration.between(o.getStartTime(), o.getEndTime()))
                .reduce(Duration.ZERO, Duration::plus);
        if (daCo.plus(thoiLuong).compareTo(GIOI_HAN_MOT_THANG) > 0) {
            throw new BusinessException("workDate", "Vượt giới hạn 40 giờ làm thêm mỗi tháng: nhân viên đã có "
                    + ThoiGianUtil.dinhDangSoGio(daCo) + ", lệnh này " + ThoiGianUtil.dinhDangSoGio(thoiLuong));
        }

        OvertimeOrder lenh = new OvertimeOrder();
        lenh.setUser(nhanVien);
        lenh.setCreatedBy(nguoiTao);
        lenh.setWorkDate(ngay);
        lenh.setStartTime(tu);
        lenh.setEndTime(den);
        lenh.setReason(form.getReason().trim());
        try {
            // Ghi ngay để chỉ mục duy nhất có điều kiện chặn trường hợp hai người cùng tạo một lúc
            overtimeOrderRepository.saveAndFlush(lenh);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("workDate", "Nhân viên đã có lệnh làm thêm còn hiệu lực vào ngày này");
        }
    }

    // Nhân viên đồng ý hoặc từ chối lệnh giao cho mình
    @Transactional
    public void phanHoi(String emailNhanVien, Long id, boolean dongY, String ghiChu) {
        String ghiChuSach = chuanHoaGhiChu(ghiChu);
        // Lấy lệnh có khóa, chỉ nhận lệnh giao cho chính mình
        OvertimeOrder lenh = overtimeOrderRepository.timDeXuLy(id)
                .filter(o -> o.getUser().getEmail().equalsIgnoreCase(emailNhanVien))
                .orElseThrow(() -> new BusinessException("Không tìm thấy lệnh làm thêm có mã " + id));
        if (lenh.getStatus() != OvertimeStatus.PENDING) {
            throw new BusinessException("Lệnh này đã được xử lý, trạng thái hiện tại: " + lenh.getStatus().getLabel());
        }
        if (khungGioDaQua(lenh, LocalDateTime.now(clock))) {
            throw new BusinessException("Khung giờ của lệnh đã qua, không thể phản hồi");
        }
        lenh.setStatus(dongY ? OvertimeStatus.ACCEPTED : OvertimeStatus.DECLINED);
        lenh.setResponseNote(ghiChuSach);
        lenh.setRespondedAt(LocalDateTime.now(clock));
    }

    // Người tạo hủy lệnh của mình
    @Transactional
    public void huyLenh(String emailNguoiTao, Long id) {
        OvertimeOrder lenh = overtimeOrderRepository.timDeXuLy(id)
                .filter(o -> o.getCreatedBy().getEmail().equalsIgnoreCase(emailNguoiTao))
                .orElseThrow(() -> new BusinessException("Không tìm thấy lệnh làm thêm có mã " + id));
        if (!CON_HIEU_LUC.contains(lenh.getStatus())) {
            throw new BusinessException("Chỉ hủy được lệnh đang chờ phản hồi hoặc đã được đồng ý");
        }
        if (lenh.getWorkDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("Lệnh đã qua ngày làm việc, không thể hủy");
        }
        if (attendanceRepository.existsByOvertimeOrderId(id)) {
            throw new BusinessException("Nhân viên đã chấm công theo lệnh này, không thể hủy");
        }
        lenh.setStatus(OvertimeStatus.CANCELLED);
    }

    // Ai được tạo lệnh cho ai: quản lý trực tiếp, hoặc ADMIN nếu nhân viên không có quản lý
    private void kiemTraQuyenTaoLenh(User nguoiTao, User nhanVien) {
        if (!nhanVien.isActive()) {
            throw new BusinessException("employeeId", "Nhân viên này đã bị khóa");
        }
        if (nguoiTao.getId().equals(nhanVien.getId())) {
            throw new BusinessException("employeeId", "Bạn không thể tạo lệnh làm thêm cho chính mình");
        }
        User quanLy = nhanVien.getManager();
        boolean duocTao = quanLy != null
                ? quanLy.getId().equals(nguoiTao.getId())
                : nguoiTao.getRole() == Role.ADMIN;
        if (!duocTao) {
            throw new BusinessException("employeeId", "Bạn không phải người quản lý của nhân viên này");
        }
    }

    private boolean khungGioDaQua(OvertimeOrder lenh, LocalDateTime bayGio) {
        return LocalDateTime.of(lenh.getWorkDate(), lenh.getEndTime()).isBefore(bayGio);
    }

    // Cắt khoảng trắng, kiểm tra độ dài, trả về null nếu để trống
    private String chuanHoaGhiChu(String ghiChu) {
        String sach = ghiChu == null ? "" : ghiChu.trim();
        if (sach.length() > GHI_CHU_TOI_DA) {
            throw new BusinessException("Ghi chú tối đa " + GHI_CHU_TOI_DA + " ký tự");
        }
        return sach.isEmpty() ? null : sach;
    }

    private User timNguoiDung(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
    }

    private OvertimeRow chuyenThanhDong(OvertimeOrder lenh, LocalDateTime bayGio) {
        OvertimeStatus trangThai = lenh.getStatus();
        LocalDate ngay = lenh.getWorkDate();
        return new OvertimeRow(
                lenh.getId(),
                lenh.getUser().getFullName(),
                lenh.getCreatedBy().getFullName(),
                ngay.format(NGAY),
                ThoiGianUtil.tenThu(ngay.getDayOfWeek()),
                lenh.getStartTime().format(GIO) + " - " + lenh.getEndTime().format(GIO),
                ThoiGianUtil.dinhDangSoGio(Duration.between(lenh.getStartTime(), lenh.getEndTime())),
                lenh.getReason(),
                trangThai.getLabel(),
                trangThai.getBadge(),
                lenh.getResponseNote(),
                trangThai == OvertimeStatus.PENDING && !khungGioDaQua(lenh, bayGio),
                CON_HIEU_LUC.contains(trangThai) && !ngay.isBefore(bayGio.toLocalDate()));
    }
}
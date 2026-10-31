package com.phuong.lams.service;

import com.phuong.lams.dto.LeaveRequestForm;
import com.phuong.lams.dto.LeaveRequestRow;
import com.phuong.lams.dto.LeaveTypeOption;
import com.phuong.lams.entity.LeaveBalance;
import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.entity.LeaveType;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.LeaveBalanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.LeaveTypeRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Nghiệp vụ xin nghỉ phép của nhân viên
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;

    // Các đơn của người có email này, mới nhất lên đầu
    @Transactional(readOnly = true)
    public List<LeaveRequestRow> layCuaToi(String email) {
        return leaveRequestRepository.layCuaNguoiDung(email).stream()
                .map(this::chuyenThanhDong)
                .toList();
    }

    // Các loại nghỉ người này được chọn khi xin nghỉ: đang dùng, áp dụng cho họ, kèm số ngày còn lại
    @Transactional(readOnly = true)
    public List<LeaveTypeOption> layLoaiNghiChoPhep(String email) {
        return leaveBalanceRepository.layTheoEmailVaNam(email, Year.now().getValue()).stream()
                .filter(b -> b.getLeaveType().isActive()
                        && b.getLeaveType().getAppliesTo().apDungCho(b.getUser().getGender()))
                .map(b -> new LeaveTypeOption(
                        b.getLeaveType().getId(),
                        b.getLeaveType().getName() + " (còn " + b.getRemainingDays() + " ngày)"))
                .toList();
    }

    @Transactional
    public void taoDon(String email, LeaveRequestForm form) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
        LocalDate tuNgay = form.getStartDate();
        LocalDate denNgay = form.getEndDate();

        // Luật về ngày
        if (denNgay.isBefore(tuNgay)) {
            throw new BusinessException("endDate", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        if (tuNgay.isBefore(LocalDate.now())) {
            throw new BusinessException("startDate", "Ngày bắt đầu không được ở quá khứ");
        }
        if (tuNgay.getYear() != denNgay.getYear()) {
            throw new BusinessException("endDate", "Đơn nghỉ không được kéo dài sang năm khác. Hãy tách thành hai đơn");
        }
        int nam = tuNgay.getYear();

        // Luật về loại nghỉ
        LeaveType loai = leaveTypeRepository.findById(form.getLeaveTypeId())
                .orElseThrow(() -> new BusinessException("leaveTypeId", "Loại nghỉ không tồn tại"));
        if (!loai.isActive() || !loai.getAppliesTo().apDungCho(user.getGender())) {
            throw new BusinessException("leaveTypeId", "Loại nghỉ này không áp dụng cho bạn");
        }

        // Số ngày do server tự tính
        int soNgay = demNgayLamViec(tuNgay, denNgay);
        if (soNgay == 0) {
            throw new BusinessException("startDate",
                    "Khoảng thời gian chọn chỉ có thứ Bảy và Chủ nhật, không có ngày làm việc nào");
        }

        // Không trùng với đơn đang chờ hoặc đã duyệt của chính mình
        long soDonTrung = leaveRequestRepository.demDonTrungNgay(
                user.getId(), List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED), tuNgay, denNgay);
        if (soDonTrung > 0) {
            throw new BusinessException("startDate",
                    "Bạn đã có đơn nghỉ (chờ duyệt hoặc đã duyệt) trùng với khoảng thời gian này");
        }

        // Đủ ngày phép: số còn lại trừ đi các ngày đang chờ duyệt phải đủ cho đơn này
        LeaveBalance soDu = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(user.getId(), loai.getId(), nam)
                .orElseThrow(() -> new BusinessException("leaveTypeId",
                        "Bạn chưa được cấp số ngày phép loại này cho năm " + nam));
        long dangCho = leaveRequestRepository.tongNgayTheoTrangThai(
                user.getId(), loai.getId(), LeaveStatus.PENDING,
                LocalDate.of(nam, 1, 1), LocalDate.of(nam, 12, 31));
        long khaDung = soDu.getRemainingDays() - dangCho;
        if (soNgay > khaDung) {
            throw new BusinessException("leaveTypeId", "Không đủ ngày phép: còn " + soDu.getRemainingDays()
                    + " ngày, đang chờ duyệt " + dangCho + " ngày, bạn xin " + soNgay + " ngày");
        }

        LeaveRequest don = new LeaveRequest();
        don.setUser(user);
        don.setLeaveType(loai);
        don.setStartDate(tuNgay);
        don.setEndDate(denNgay);
        don.setDays(soNgay);
        don.setReason(form.getReason().trim());
        leaveRequestRepository.save(don);
    }

    // Người xin nghỉ tự hủy đơn của mình, chỉ khi đơn còn chờ duyệt
    @Transactional
    public void huyDon(String email, Long id) {
        // Đơn không tồn tại và đơn của người khác cùng báo một thông điệp, để không lộ thông tin
        LeaveRequest don = leaveRequestRepository.findById(id)
                .filter(d -> d.getUser().getEmail().equalsIgnoreCase(email))
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn nghỉ phép có mã " + id));
        if (don.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Chỉ hủy được đơn đang chờ duyệt");
        }
        don.setStatus(LeaveStatus.CANCELLED);
    }

    // Đếm số ngày làm việc trong khoảng [tuNgay, denNgay], bỏ thứ Bảy và Chủ nhật
    private int demNgayLamViec(LocalDate tuNgay, LocalDate denNgay) {
        int dem = 0;
        for (LocalDate ngay = tuNgay; !ngay.isAfter(denNgay); ngay = ngay.plusDays(1)) {
            DayOfWeek thu = ngay.getDayOfWeek();
            if (thu != DayOfWeek.SATURDAY && thu != DayOfWeek.SUNDAY) {
                dem++;
            }
        }
        return dem;
    }

    private LeaveRequestRow chuyenThanhDong(LeaveRequest don) {
        LeaveStatus trangThai = don.getStatus();
        return new LeaveRequestRow(
                don.getId(),
                don.getLeaveType().getName(),
                don.getStartDate().format(NGAY),
                don.getEndDate().format(NGAY),
                don.getDays(),
                don.getReason(),
                trangThai.getLabel(),
                trangThai.getBadge(),
                don.getReviewNote(),
                trangThai == LeaveStatus.PENDING);
    }
}
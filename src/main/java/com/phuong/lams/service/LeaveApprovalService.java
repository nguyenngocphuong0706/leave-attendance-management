package com.phuong.lams.service;

import com.phuong.lams.dto.ApprovalRow;
import com.phuong.lams.entity.LeaveBalance;
import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.LeaveStatus;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.LeaveBalanceRepository;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Nghiệp vụ duyệt và từ chối đơn nghỉ phép dành cho quản lý
@Service
@RequiredArgsConstructor
public class LeaveApprovalService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int GHI_CHU_TOI_DA = 500;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;

    // Các đơn đang chờ mà người này có quyền duyệt, đơn gửi sớm nhất lên đầu
    @Transactional(readOnly = true)
    public List<ApprovalRow> layChoDuyet(String email) {
        User nguoiDuyet = timNguoiDuyet(email);

        // Đơn của những nhân viên do mình làm quản lý trực tiếp
        List<LeaveRequest> danhSach = new ArrayList<>(
                leaveRequestRepository.layChoDuyetCuaQuanLy(nguoiDuyet.getId(), LeaveStatus.PENDING));
        // ADMIN duyệt thêm đơn của những người không có quản lý
        if (nguoiDuyet.getRole() == Role.ADMIN) {
            danhSach.addAll(leaveRequestRepository.layChoDuyetKhongCoQuanLy(nguoiDuyet.getId(), LeaveStatus.PENDING));
        }
        danhSach.sort(Comparator.comparing(LeaveRequest::getCreatedAt));

        return danhSach.stream().map(this::chuyenThanhDong).toList();
    }

    // Duyệt đơn: trừ ngày phép và đổi trạng thái trong cùng một transaction
    @Transactional
    public void duyet(String emailNguoiDuyet, Long donId, String ghiChu) {
        String ghiChuSach = chuanHoaGhiChu(ghiChu, false);
        User nguoiDuyet = timNguoiDuyet(emailNguoiDuyet);
        LeaveRequest don = layDonDeXuLy(nguoiDuyet, donId);

        // Khóa dòng số dư rồi mới đọc, để hai đơn duyệt cùng lúc không ghi đè nhau
        int nam = don.getStartDate().getYear();
        LeaveBalance soDu = leaveBalanceRepository
                .timDeTruNgay(don.getUser().getId(), don.getLeaveType().getId(), nam)
                .orElseThrow(() -> new BusinessException(
                        "Người xin nghỉ chưa được cấp số ngày phép loại này cho năm " + nam));
        if (soDu.getRemainingDays() < don.getDays()) {
            throw new BusinessException("Không đủ ngày phép để duyệt: còn " + soDu.getRemainingDays()
                    + " ngày, đơn xin " + don.getDays() + " ngày");
        }

        soDu.setUsedDays(soDu.getUsedDays() + don.getDays());
        ketThuc(don, LeaveStatus.APPROVED, nguoiDuyet, ghiChuSach);
    }

    // Từ chối đơn: bắt buộc ghi lý do, không đụng tới số dư
    @Transactional
    public void tuChoi(String emailNguoiDuyet, Long donId, String ghiChu) {
        String ghiChuSach = chuanHoaGhiChu(ghiChu, true);
        User nguoiDuyet = timNguoiDuyet(emailNguoiDuyet);
        LeaveRequest don = layDonDeXuLy(nguoiDuyet, donId);

        ketThuc(don, LeaveStatus.REJECTED, nguoiDuyet, ghiChuSach);
    }

    // Lấy đơn (có khóa), kiểm tra người duyệt có quyền và đơn còn đang chờ
    private LeaveRequest layDonDeXuLy(User nguoiDuyet, Long donId) {
        LeaveRequest don = leaveRequestRepository.timDeXuLy(donId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn nghỉ phép có mã " + donId));
        kiemTraQuyenDuyet(nguoiDuyet, don);
        if (don.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Đơn này đã được xử lý, trạng thái hiện tại: " + don.getStatus().getLabel());
        }
        return don;
    }

    // Ai được duyệt đơn của ai
    private void kiemTraQuyenDuyet(User nguoiDuyet, LeaveRequest don) {
        User nguoiXin = don.getUser();
        if (!nguoiDuyet.isActive() || nguoiDuyet.getRole() == Role.EMPLOYEE) {
            throw new BusinessException("Bạn không có quyền duyệt đơn nghỉ phép");
        }
        if (nguoiDuyet.getId().equals(nguoiXin.getId())) {
            throw new BusinessException("Bạn không thể tự duyệt đơn của chính mình");
        }
        User quanLy = nguoiXin.getManager();
        // Có quản lý trực tiếp thì chỉ người đó duyệt, không có thì ADMIN duyệt
        boolean duocDuyet = quanLy != null
                ? quanLy.getId().equals(nguoiDuyet.getId())
                : nguoiDuyet.getRole() == Role.ADMIN;
        if (!duocDuyet) {
            throw new BusinessException("Bạn không phải người duyệt đơn của nhân viên này");
        }
    }

    // Ghi kết quả xử lý lên đơn
    private void ketThuc(LeaveRequest don, LeaveStatus trangThai, User nguoiDuyet, String ghiChu) {
        don.setStatus(trangThai);
        don.setReviewer(nguoiDuyet);
        don.setReviewNote(ghiChu);
        don.setReviewedAt(LocalDateTime.now());
    }

    // Cắt khoảng trắng, kiểm tra độ dài, trả về null nếu để trống
    private String chuanHoaGhiChu(String ghiChu, boolean batBuoc) {
        String sach = ghiChu == null ? "" : ghiChu.trim();
        if (batBuoc && sach.isEmpty()) {
            throw new BusinessException("Hãy nhập lý do khi từ chối đơn");
        }
        if (sach.length() > GHI_CHU_TOI_DA) {
            throw new BusinessException("Ghi chú tối đa " + GHI_CHU_TOI_DA + " ký tự");
        }
        return sach.isEmpty() ? null : sach;
    }

    private User timNguoiDuyet(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản đang đăng nhập"));
    }

    private ApprovalRow chuyenThanhDong(LeaveRequest don) {
        User nguoiXin = don.getUser();
        return new ApprovalRow(
                don.getId(),
                nguoiXin.getFullName(),
                nguoiXin.getDepartment() != null ? nguoiXin.getDepartment().getName() : "Chưa có",
                don.getLeaveType().getName(),
                don.getStartDate().format(NGAY),
                don.getEndDate().format(NGAY),
                don.getDays(),
                don.getReason(),
                don.getCreatedAt().format(NGAY_GIO));
    }
}
package com.phuong.lams.service;

import com.phuong.lams.dto.LeaveBalanceRow;
import com.phuong.lams.entity.LeaveBalance;
import com.phuong.lams.entity.LeaveType;
import com.phuong.lams.entity.User;
import com.phuong.lams.repository.LeaveBalanceRepository;
import com.phuong.lams.repository.LeaveTypeRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

// Nghiệp vụ liên quan đến số ngày phép
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;

    // Số dư phép năm hiện tại của người có email này, chỉ gồm những dòng nên hiển thị
    @Transactional(readOnly = true)
    public List<LeaveBalanceRow> laySoDuNamNay(String email) {
        return leaveBalanceRepository.layTheoEmailVaNam(email, Year.now().getValue()).stream()
                .filter(this::nenHienThi)
                .map(b -> new LeaveBalanceRow(
                        b.getLeaveType().getName(),
                        b.getTotalDays(),
                        b.getUsedDays(),
                        b.getRemainingDays()))
                .toList();
    }

    // Cấp số dư năm nay cho một người, với mọi loại nghỉ đang dùng và áp dụng cho người đó
    @Transactional
    public void capSoDuChoNguoiDung(User user) {
        int nam = Year.now().getValue();
        for (LeaveType loai : leaveTypeRepository.findByActiveTrueOrderByIdAsc()) {
            capNeuChuaCo(user, loai, nam);
        }
    }

    // Cấp số dư năm nay cho những người thuộc đối tượng của loại nghỉ này
    @Transactional
    public void capSoDuChoLoaiNghi(LeaveType loai) {
        int nam = Year.now().getValue();
        for (User user : userRepository.findAll()) {
            capNeuChuaCo(user, loai, nam);
        }
    }

    // Đã có người dùng ngày phép của loại nghỉ này chưa
    @Transactional(readOnly = true)
    public boolean coNguoiDaDung(Long loaiNghiId) {
        return leaveBalanceRepository.existsByLeaveTypeIdAndUsedDaysGreaterThan(loaiNghiId, 0);
    }

    // Xóa số dư của một loại nghỉ, chỉ gọi khi chưa ai dùng
    @Transactional
    public void xoaSoDuCuaLoaiNghi(Long loaiNghiId) {
        leaveBalanceRepository.xoaTheoLoaiNghi(loaiNghiId);
    }

    // Hiện khi loại nghỉ đang dùng và áp dụng cho người này, hoặc khi người này đã dùng ngày phép
    // (đã dùng thì là dữ liệu có thật, không được giấu đi)
    private boolean nenHienThi(LeaveBalance b) {
        LeaveType loai = b.getLeaveType();
        boolean apDung = loai.isActive() && loai.getAppliesTo().apDungCho(b.getUser().getGender());
        return apDung || b.getUsedDays() > 0;
    }

    // Chỉ cấp khi đúng đối tượng và chưa có, nên gọi nhiều lần cũng không bị trùng
    private void capNeuChuaCo(User user, LeaveType loai, int nam) {
        if (!loai.getAppliesTo().apDungCho(user.getGender())) {
            return;
        }
        if (leaveBalanceRepository.existsByUserIdAndLeaveTypeIdAndYear(user.getId(), loai.getId(), nam)) {
            return;
        }
        LeaveBalance soDu = new LeaveBalance();
        soDu.setUser(user);
        soDu.setLeaveType(loai);
        soDu.setYear(nam);
        soDu.setTotalDays(loai.getDaysPerYear());
        leaveBalanceRepository.save(soDu);
    }
}
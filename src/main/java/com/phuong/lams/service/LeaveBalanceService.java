package com.phuong.lams.service;

import com.phuong.lams.dto.LeaveBalanceRow;
import com.phuong.lams.repository.LeaveBalanceRepository;
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

    // Số dư phép năm hiện tại của người có email này
    @Transactional(readOnly = true)
    public List<LeaveBalanceRow> laySoDuNamNay(String email) {
        return leaveBalanceRepository.layTheoEmailVaNam(email, Year.now().getValue()).stream()
                .map(b -> new LeaveBalanceRow(
                        b.getLeaveType().getName(),
                        b.getTotalDays(),
                        b.getUsedDays(),
                        b.getRemainingDays()))
                .toList();
    }
}
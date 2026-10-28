package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Truy vấn bảng leave_balances (số dư phép)
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    // Số dư của một người trong một năm, lấy kèm loại nghỉ bằng một câu truy vấn
    @Query("""
            select b from LeaveBalance b
            join fetch b.leaveType
            where b.user.email = :email and b.year = :year
            order by b.leaveType.id
            """)
    List<LeaveBalance> layTheoEmailVaNam(@Param("email") String email, @Param("year") int year);
}
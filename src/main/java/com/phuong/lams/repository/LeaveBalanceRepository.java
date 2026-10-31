package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Truy vấn bảng leave_balances (số dư phép)
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    // Số dư của một người trong một năm, lấy kèm loại nghỉ và người dùng bằng một câu truy vấn
    @Query("""
            select b from LeaveBalance b
            join fetch b.leaveType lt
            join fetch b.user u
            where u.email = :email and b.year = :year
            order by lt.id
            """)
    List<LeaveBalance> layTheoEmailVaNam(@Param("email") String email, @Param("year") int year);

    // Số dư của một người cho một loại nghỉ trong một năm
    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, int year);

    // Như trên nhưng KHÓA dòng số dư đến hết transaction, dùng khi sắp trừ ngày phép
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from LeaveBalance b
            where b.user.id = :userId and b.leaveType.id = :leaveTypeId and b.year = :year
            """)
    Optional<LeaveBalance> timDeTruNgay(@Param("userId") Long userId,
                                        @Param("leaveTypeId") Long leaveTypeId,
                                        @Param("year") int year);

    // Người này đã có số dư cho loại nghỉ này trong năm này chưa
    boolean existsByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId, int year);

    // Đã có ai dùng ngày phép của loại nghỉ này chưa (dùng khi quyết định có cho xóa không)
    boolean existsByLeaveTypeIdAndUsedDaysGreaterThan(Long leaveTypeId, int usedDays);

    // Xóa toàn bộ số dư của một loại nghỉ (chỉ gọi khi chưa ai dùng)
    @Modifying
    @Query("delete from LeaveBalance b where b.leaveType.id = :leaveTypeId")
    void xoaTheoLoaiNghi(@Param("leaveTypeId") Long leaveTypeId);
}
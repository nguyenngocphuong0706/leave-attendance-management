package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

// Truy vấn bảng leave_requests (đơn xin nghỉ phép)
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Các đơn của một người, mới nhất lên đầu, lấy kèm loại nghỉ bằng một câu truy vấn
    @Query("""
            select r from LeaveRequest r
            join fetch r.leaveType
            where r.user.email = :email
            order by r.createdAt desc, r.id desc
            """)
    List<LeaveRequest> layCuaNguoiDung(@Param("email") String email);

    // Đếm số đơn của người này có trạng thái cho trước và trùng khoảng ngày [tuNgay, denNgay]
    // Hai khoảng ngày trùng nhau khi: bắt đầu_A <= kết thúc_B và kết thúc_A >= bắt đầu_B
    @Query("""
            select count(r) from LeaveRequest r
            where r.user.id = :userId
              and r.status in :trangThai
              and r.startDate <= :denNgay
              and r.endDate >= :tuNgay
            """)
    long demDonTrungNgay(@Param("userId") Long userId,
                         @Param("trangThai") Collection<LeaveStatus> trangThai,
                         @Param("tuNgay") LocalDate tuNgay,
                         @Param("denNgay") LocalDate denNgay);

    // Tổng số ngày của các đơn có trạng thái cho trước, theo người, loại nghỉ, trong một khoảng ngày
    @Query("""
            select coalesce(sum(r.days), 0L) from LeaveRequest r
            where r.user.id = :userId
              and r.leaveType.id = :leaveTypeId
              and r.status = :trangThai
              and r.startDate >= :tuNgay
              and r.endDate <= :denNgay
            """)
    long tongNgayTheoTrangThai(@Param("userId") Long userId,
                               @Param("leaveTypeId") Long leaveTypeId,
                               @Param("trangThai") LeaveStatus trangThai,
                               @Param("tuNgay") LocalDate tuNgay,
                               @Param("denNgay") LocalDate denNgay);
}
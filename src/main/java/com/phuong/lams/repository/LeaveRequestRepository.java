package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveRequest;
import com.phuong.lams.enums.LeaveStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    // Các đơn đang chờ của những nhân viên do người này làm quản lý trực tiếp
    @Query("""
            select r from LeaveRequest r
            join fetch r.user u
            join fetch r.leaveType
            left join fetch u.department
            where r.status = :trangThai
              and u.manager.id = :nguoiDuyetId
            order by r.createdAt asc
            """)
    List<LeaveRequest> layChoDuyetCuaQuanLy(@Param("nguoiDuyetId") Long nguoiDuyetId,
                                            @Param("trangThai") LeaveStatus trangThai);

    // Các đơn đang chờ của những người không có quản lý trực tiếp (ADMIN duyệt), trừ đơn của chính người duyệt
    @Query("""
            select r from LeaveRequest r
            join fetch r.user u
            join fetch r.leaveType
            left join fetch u.department
            where r.status = :trangThai
              and u.manager is null
              and u.id <> :nguoiDuyetId
            order by r.createdAt asc
            """)
    List<LeaveRequest> layChoDuyetKhongCoQuanLy(@Param("nguoiDuyetId") Long nguoiDuyetId,
                                                @Param("trangThai") LeaveStatus trangThai);

    // Lấy một đơn và KHÓA dòng đó đến hết transaction. Ai khác muốn xử lý cùng đơn phải chờ
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LeaveRequest r where r.id = :id")
    Optional<LeaveRequest> timDeXuLy(@Param("id") Long id);

    // Đã có đơn nghỉ nào thuộc loại nghỉ này chưa (dùng khi quyết định có cho xóa loại nghỉ không)
    boolean existsByLeaveTypeId(Long leaveTypeId);
}
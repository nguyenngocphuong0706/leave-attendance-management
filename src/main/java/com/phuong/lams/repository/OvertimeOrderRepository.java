package com.phuong.lams.repository;

import com.phuong.lams.entity.OvertimeOrder;
import com.phuong.lams.enums.OvertimeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Truy vấn bảng overtime_orders (lệnh làm thêm giờ)
public interface OvertimeOrderRepository extends JpaRepository<OvertimeOrder, Long> {

    // Các lệnh giao cho một nhân viên, ngày gần nhất lên đầu
    @Query("""
            select o from OvertimeOrder o
            join fetch o.user u
            join fetch o.createdBy c
            where u.email = :email
            order by o.workDate desc, o.id desc
            """)
    List<OvertimeOrder> layCuaNhanVien(@Param("email") String email);

    // Các lệnh do một người tạo
    @Query("""
            select o from OvertimeOrder o
            join fetch o.user u
            join fetch o.createdBy c
            where c.email = :email
            order by o.workDate desc, o.id desc
            """)
    List<OvertimeOrder> layDoNguoiTao(@Param("email") String email);

    // Nhân viên đã có lệnh với trạng thái cho trước vào ngày này chưa
    boolean existsByUserIdAndWorkDateAndStatusIn(Long userId, LocalDate workDate,
                                                 Collection<OvertimeStatus> trangThai);

    // Các lệnh của một nhân viên trong khoảng ngày (dùng để cộng giờ trong tháng)
    List<OvertimeOrder> findByUserIdAndWorkDateBetweenAndStatusIn(Long userId, LocalDate tuNgay,
                                                                  LocalDate denNgay,
                                                                  Collection<OvertimeStatus> trangThai);

    // Lệnh của một nhân viên vào một ngày với trạng thái cho trước (chỉ để xem)
    Optional<OvertimeOrder> findByUserIdAndWorkDateAndStatus(Long userId, LocalDate workDate,
                                                             OvertimeStatus trangThai);

    // Lấy một lệnh và KHÓA dòng đó đến hết transaction (dùng khi phản hồi, hủy)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OvertimeOrder o where o.id = :id")
    Optional<OvertimeOrder> timDeXuLy(@Param("id") Long id);

    // Như trên nhưng tìm theo nhân viên, ngày, trạng thái (dùng khi check-in)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OvertimeOrder o
            where o.user.id = :userId and o.workDate = :ngay and o.status = :trangThai
            """)
    Optional<OvertimeOrder> timDeChamCong(@Param("userId") Long userId,
                                          @Param("ngay") LocalDate ngay,
                                          @Param("trangThai") OvertimeStatus trangThai);
}
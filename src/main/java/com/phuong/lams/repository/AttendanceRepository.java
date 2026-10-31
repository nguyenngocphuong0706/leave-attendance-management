package com.phuong.lams.repository;

import com.phuong.lams.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Truy vấn bảng attendance_records (chấm công)
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    // Bản ghi chấm công của một người trong một ngày (có hoặc không)
    Optional<AttendanceRecord> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

    // Các bản ghi của một người trong khoảng ngày, ngày mới nhất lên đầu
    @Query("""
            select a from AttendanceRecord a
            where a.user.email = :email
              and a.workDate between :tuNgay and :denNgay
            order by a.workDate desc
            """)
    List<AttendanceRecord> layTheoKhoang(@Param("email") String email,
                                         @Param("tuNgay") LocalDate tuNgay,
                                         @Param("denNgay") LocalDate denNgay);
}
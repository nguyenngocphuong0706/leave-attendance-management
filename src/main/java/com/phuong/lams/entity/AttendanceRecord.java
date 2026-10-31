package com.phuong.lams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Ánh xạ với bảng "attendance_records" (chấm công một người trong một ngày)
@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    // Trống cho đến khi nhân viên check-out
    @Column(name = "check_out")
    private LocalDateTime checkOut;

    // Đi muộn, được xác định lúc check-in
    @Column(nullable = false)
    private boolean late;

    // Về sớm, được xác định lúc check-out
    @Column(name = "left_early", nullable = false)
    private boolean leftEarly;
}
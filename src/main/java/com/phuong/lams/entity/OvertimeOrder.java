package com.phuong.lams.entity;

import com.phuong.lams.enums.OvertimeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Ánh xạ với bảng "overtime_orders" (lệnh làm thêm giờ)
@Entity
@Table(name = "overtime_orders")
@Getter
@Setter
@NoArgsConstructor
public class OvertimeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nhân viên được yêu cầu làm thêm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Người tạo lệnh
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OvertimeStatus status = OvertimeStatus.PENDING;

    @Column(name = "response_note", length = 500)
    private String responseNote;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự gán thời gian tạo ngay trước khi lưu lần đầu
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Thời gian làm thực tế nằm trong khung giờ của lệnh, phần làm ngoài khung không được tính
    public Duration thoiGianTrongKhung(LocalDateTime vao, LocalDateTime ra) {
        LocalDateTime dauKhung = LocalDateTime.of(workDate, startTime);
        LocalDateTime cuoiKhung = LocalDateTime.of(workDate, endTime);
        LocalDateTime batDau = vao.isAfter(dauKhung) ? vao : dauKhung;
        LocalDateTime ketThuc = ra.isBefore(cuoiKhung) ? ra : cuoiKhung;
        return ketThuc.isAfter(batDau) ? Duration.between(batDau, ketThuc) : Duration.ZERO;
    }
}
package com.phuong.lams.entity;

import com.phuong.lams.enums.Gender;
import com.phuong.lams.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Ánh xạ với bảng "users" (nhân viên)
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Mật khẩu đã băm, không bao giờ lưu mật khẩu thô
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Giới tính, dùng để xác định loại nghỉ nào áp dụng (có thể chưa có với dữ liệu cũ)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    // Lưu vai trò dưới dạng chữ (EMPLOYEE, MANAGER, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Nhiều nhân viên thuộc một phòng ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Người quản lý trực tiếp (cũng là một User)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    // Tài khoản còn hoạt động hay không
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự gán thời gian tạo ngay trước khi lưu lần đầu
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
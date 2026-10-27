package com.phuong.lams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Ánh xạ với bảng "departments" (phòng ban)
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    // Khóa chính, database tự tăng (BIGSERIAL)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên phòng ban: bắt buộc, không trùng
    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
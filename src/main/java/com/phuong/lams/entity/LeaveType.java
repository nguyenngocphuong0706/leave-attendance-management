package com.phuong.lams.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Ánh xạ với bảng "leave_types" (loại nghỉ phép)
@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Số ngày được cấp mỗi năm cho loại nghỉ này
    @Column(name = "days_per_year", nullable = false)
    private int daysPerYear;

    // Loại nghỉ còn được sử dụng hay không
    @Column(nullable = false)
    private boolean active = true;
}
package com.phuong.lams.repository;

import com.phuong.lams.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Truy vấn bảng departments (phòng ban)
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Tìm phòng ban theo tên
    Optional<Department> findByName(String name);
}
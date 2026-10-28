package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Truy vấn bảng leave_types (loại nghỉ phép)
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    // Các loại nghỉ còn được sử dụng
    List<LeaveType> findByActiveTrueOrderByIdAsc();
}
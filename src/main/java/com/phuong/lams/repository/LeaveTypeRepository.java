package com.phuong.lams.repository;

import com.phuong.lams.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Truy vấn bảng leave_types (loại nghỉ phép)
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    // Các loại nghỉ còn được sử dụng
    List<LeaveType> findByActiveTrueOrderByIdAsc();

    // Tất cả loại nghỉ, kể cả loại đã ngừng dùng
    List<LeaveType> findAllByOrderByIdAsc();

    // Kiểm tra tên đã tồn tại chưa (không phân biệt hoa thường)
    boolean existsByNameIgnoreCase(String name);

    // Như trên nhưng bỏ qua chính loại nghỉ đang sửa
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
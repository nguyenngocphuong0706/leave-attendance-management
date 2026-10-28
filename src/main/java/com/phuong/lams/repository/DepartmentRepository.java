package com.phuong.lams.repository;

import com.phuong.lams.dto.DepartmentRow;
import com.phuong.lams.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// Truy vấn bảng departments (phòng ban)
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Tìm phòng ban theo tên
    Optional<Department> findByName(String name);

    // Kiểm tra tên đã tồn tại chưa (không phân biệt hoa thường)
    boolean existsByNameIgnoreCase(String name);

    // Như trên nhưng bỏ qua chính phòng ban đang sửa
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Lấy danh sách phòng ban kèm số nhân viên trong MỘT câu truy vấn duy nhất
    @Query("""
            select new com.phuong.lams.dto.DepartmentRow(d.id, d.name, count(u))
            from Department d left join User u on u.department = d
            group by d.id, d.name
            order by d.id
            """)
    List<DepartmentRow> layDanhSachKemSoNhanVien();
}
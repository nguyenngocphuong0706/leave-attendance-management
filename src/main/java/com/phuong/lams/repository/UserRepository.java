package com.phuong.lams.repository;

import com.phuong.lams.entity.User;
import com.phuong.lams.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Truy vấn bảng users (nhân viên)
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm nhân viên theo email (dùng khi đăng nhập)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Đếm số nhân viên của một phòng ban (dùng khi kiểm tra trước khi xóa phòng ban)
    long countByDepartmentId(Long departmentId);

    // Lấy tất cả người dùng kèm phòng ban và quản lý trong MỘT câu truy vấn
    @Query("""
            select u from User u
            left join fetch u.department
            left join fetch u.manager
            order by u.id
            """)
    List<User> layTatCaKemChiTiet();

    // Danh sách người có thể làm quản lý: đang hoạt động và có vai trò thuộc nhóm cho trước
    List<User> findByRoleInAndActiveTrueOrderByFullNameAsc(Collection<Role> roles);

    // Nhân viên đang hoạt động do một người làm quản lý trực tiếp
    List<User> findByManagerIdAndActiveTrueOrderByFullNameAsc(Long managerId);

    // Người đang hoạt động không có quản lý trực tiếp, trừ một người cho trước (chính người đang xét)
    List<User> findByManagerIsNullAndActiveTrueAndIdNotOrderByFullNameAsc(Long id);

    // Một quản lý và các nhân viên đang hoạt động của người đó, kèm phòng ban (dùng cho báo cáo)
    @Query("""
            select u from User u
            left join fetch u.department
            where u.active = true
              and (u.id = :id or u.manager.id = :id)
            order by u.fullName
            """)
    List<User> layNhomCuaQuanLy(@Param("id") Long id);

    // Tất cả người đang hoạt động, kèm phòng ban (dùng cho báo cáo của ADMIN)
    @Query("""
            select u from User u
            left join fetch u.department
            where u.active = true
            order by u.fullName
            """)
    List<User> layTatCaDangHoatDong();
}
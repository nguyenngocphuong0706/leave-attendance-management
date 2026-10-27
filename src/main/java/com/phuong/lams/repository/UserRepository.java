package com.phuong.lams.repository;

import com.phuong.lams.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Truy vấn bảng users (nhân viên)
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm nhân viên theo email (dùng khi đăng nhập)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
}
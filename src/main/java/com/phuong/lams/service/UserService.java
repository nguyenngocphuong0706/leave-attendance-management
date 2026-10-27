package com.phuong.lams.service;

import com.phuong.lams.dto.UserProfile;
import com.phuong.lams.entity.User;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // readOnly = true: chỉ đọc, không ghi. Cần @Transactional để truy cập được dữ liệu LAZY
    @Transactional(readOnly = true)
    public UserProfile layThongTinCaNhan(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng: " + email));

        // getDepartment() và getManager() là LAZY: đến dòng này Hibernate mới chạy SQL lấy chúng
        String phongBan = user.getDepartment() != null ? user.getDepartment().getName() : "Chưa có phòng ban";
        String quanLy = user.getManager() != null ? user.getManager().getFullName() : "Không có";

        return new UserProfile(user.getFullName(), user.getEmail(), user.getRole(), phongBan, quanLy);
    }
}
package com.phuong.lams.config;

import com.phuong.lams.entity.Department;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.Gender;
import com.phuong.lams.enums.Role;
import com.phuong.lams.repository.DepartmentRepository;
import com.phuong.lams.repository.UserRepository;
import com.phuong.lams.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Chạy một lần khi ứng dụng khởi động để nạp dữ liệu mẫu (chỉ dùng cho môi trường phát triển)
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveBalanceService leaveBalanceService;

    @Override
    @Transactional
    public void run(String... args) {
        // Đã có dữ liệu thì không nạp lại, tránh bị trùng mỗi lần chạy app
        if (userRepository.count() > 0) {
            return;
        }

        // 1. Tạo phòng ban
        Department phongIt = new Department();
        phongIt.setName("Phòng Công nghệ thông tin");
        Department phongNhanSu = new Department();
        phongNhanSu.setName("Phòng Nhân sự");
        departmentRepository.saveAll(List.of(phongIt, phongNhanSu));

        // 2. Tạo người dùng. Phải lưu quản lý trước để nhân viên trỏ tới được
        taoUser("admin@lams.com", "Quản trị viên", null, Role.ADMIN, phongNhanSu, null);
        User quanLy = taoUser("manager@lams.com", "Trần Văn Quản Lý", Gender.MALE, Role.MANAGER, phongIt, null);
        taoUser("an@lams.com", "Nguyễn Văn An", Gender.MALE, Role.EMPLOYEE, phongIt, quanLy);
        taoUser("binh@lams.com", "Lê Thị Bình", Gender.FEMALE, Role.EMPLOYEE, phongIt, quanLy);
    }

    // Hàm phụ: tạo và lưu một người dùng, mật khẩu mẫu là 123456
    private User taoUser(String email, String hoTen, Gender gioiTinh, Role vaiTro,
                         Department phongBan, User quanLy) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(hoTen);
        user.setGender(gioiTinh);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(vaiTro);
        user.setDepartment(phongBan);
        user.setManager(quanLy);
        User daLuu = userRepository.save(user);
        // Cấp ngay số ngày phép năm nay theo các loại nghỉ đã có từ migration
        leaveBalanceService.capSoDuChoNguoiDung(daLuu);
        return daLuu;
    }
}
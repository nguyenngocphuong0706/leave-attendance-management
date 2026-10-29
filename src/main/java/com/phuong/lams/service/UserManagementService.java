package com.phuong.lams.service;

import com.phuong.lams.dto.UserForm;
import com.phuong.lams.dto.UserOption;
import com.phuong.lams.dto.UserRow;
import com.phuong.lams.entity.Department;
import com.phuong.lams.entity.User;
import com.phuong.lams.enums.Role;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.DepartmentRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Nghiệp vụ quản lý người dùng dành cho ADMIN
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final int MAT_KHAU_TOI_THIEU = 6;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveBalanceService leaveBalanceService;

    @Transactional(readOnly = true)
    public List<UserRow> layDanhSach() {
        return userRepository.layTatCaKemChiTiet().stream()
                .map(u -> new UserRow(
                        u.getId(),
                        u.getFullName(),
                        u.getGender() != null ? u.getGender().getLabel() : "Chưa có",
                        u.getEmail(),
                        u.getRole(),
                        u.getDepartment() != null ? u.getDepartment().getName() : "Chưa có",
                        u.getManager() != null ? u.getManager().getFullName() : "Không có",
                        u.isActive()))
                .toList();
    }

    // Những người có thể được chọn làm quản lý, loại trừ chính người đang sửa
    @Transactional(readOnly = true)
    public List<UserOption> layDanhSachQuanLy(Long loaiTruId) {
        return userRepository.findByRoleInAndActiveTrueOrderByFullNameAsc(List.of(Role.MANAGER, Role.ADMIN))
                .stream()
                .filter(u -> !u.getId().equals(loaiTruId))
                .map(u -> new UserOption(u.getId(), u.getFullName() + " (" + u.getRole() + ")"))
                .toList();
    }

    // Lấy dữ liệu người dùng đưa lên form sửa
    @Transactional(readOnly = true)
    public UserForm layDeSua(Long id) {
        User user = timTheoId(id);
        UserForm form = new UserForm();
        form.setFullName(user.getFullName());
        form.setGender(user.getGender());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getId() : null);
        form.setManagerId(user.getManager() != null ? user.getManager().getId() : null);
        return form;
    }

    @Transactional
    public void them(UserForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("email", "Email \"" + email + "\" đã được sử dụng");
        }
        String matKhau = form.getPassword();
        if (matKhau == null || matKhau.length() < MAT_KHAU_TOI_THIEU) {
            throw new BusinessException("password", "Mật khẩu tối thiểu " + MAT_KHAU_TOI_THIEU + " ký tự");
        }

        User user = new User();
        user.setEmail(email);
        // Luôn băm mật khẩu trước khi lưu, không bao giờ lưu mật khẩu thô
        user.setPasswordHash(passwordEncoder.encode(matKhau));
        ganThongTin(user, form, null);
        User daLuu = userRepository.save(user);
        // Người mới được cấp ngay số ngày phép năm nay, cùng một transaction với việc tạo tài khoản
        leaveBalanceService.capSoDuChoNguoiDung(daLuu);
    }

    // Email không được sửa vì đó là tên đăng nhập, nên ở đây bỏ qua form.getEmail()
    @Transactional
    public void capNhat(Long id, UserForm form) {
        User user = timTheoId(id);
        ganThongTin(user, form, id);
        // Đổi giới tính có thể làm xuất hiện loại nghỉ mới áp dụng cho người này, nên bổ sung số dư còn thiếu
        leaveBalanceService.capSoDuChoNguoiDung(user);
    }

    // Khóa hoặc mở khóa, trả về trạng thái mới (true là đang hoạt động)
    @Transactional
    public boolean doiTrangThai(Long id, String emailNguoiDangNhap) {
        User user = timTheoId(id);
        if (user.getEmail().equalsIgnoreCase(emailNguoiDangNhap)) {
            throw new BusinessException("Không thể khóa chính tài khoản đang đăng nhập");
        }
        user.setActive(!user.isActive());
        return user.isActive();
    }

    // Gán các thông tin chung của form vào entity (dùng cho cả thêm và sửa)
    private void ganThongTin(User user, UserForm form, Long idDangSua) {
        user.setFullName(form.getFullName().trim());
        user.setGender(form.getGender());
        user.setRole(form.getRole());
        user.setDepartment(timPhongBan(form.getDepartmentId()));
        user.setManager(timQuanLy(form.getManagerId(), idDangSua));
    }

    private Department timPhongBan(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("departmentId", "Phòng ban không tồn tại"));
    }

    private User timQuanLy(Long managerId, Long idDangSua) {
        if (managerId == null) {
            return null;
        }
        if (managerId.equals(idDangSua)) {
            throw new BusinessException("managerId", "Không thể chọn chính mình làm quản lý");
        }
        User quanLy = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException("managerId", "Quản lý không tồn tại"));
        if (!quanLy.isActive() || quanLy.getRole() == Role.EMPLOYEE) {
            throw new BusinessException("managerId", "Quản lý phải là MANAGER hoặc ADMIN đang hoạt động");
        }
        return quanLy;
    }

    private User timTheoId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng có mã " + id));
    }
}
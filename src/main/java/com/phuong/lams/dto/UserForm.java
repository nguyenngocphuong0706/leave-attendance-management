package com.phuong.lams.dto;

import com.phuong.lams.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dữ liệu từ form thêm/sửa người dùng
@Getter
@Setter
@NoArgsConstructor
public class UserForm {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    // Chỉ dùng khi thêm mới. Khi sửa, mật khẩu không đổi ở form này
    private String password;

    @NotNull(message = "Hãy chọn vai trò")
    private Role role;

    // Không bắt buộc: có thể chưa xếp phòng ban
    private Long departmentId;

    // Không bắt buộc: admin và quản lý thường không có quản lý trực tiếp
    private Long managerId;
}
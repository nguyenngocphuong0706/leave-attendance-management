package com.phuong.lams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dữ liệu từ form thêm/sửa phòng ban
@Getter
@Setter
@NoArgsConstructor
public class DepartmentForm {

    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 100, message = "Tên phòng ban tối đa 100 ký tự")
    private String name;
}
package com.phuong.lams.dto;

import com.phuong.lams.enums.Applicability;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dữ liệu từ form thêm/sửa loại nghỉ phép
@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeForm {

    @NotBlank(message = "Tên loại nghỉ không được để trống")
    @Size(max = 100, message = "Tên loại nghỉ tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Hãy nhập số ngày mỗi năm")
    @Min(value = 1, message = "Số ngày tối thiểu là 1")
    @Max(value = 365, message = "Số ngày tối đa 365")
    private Integer daysPerYear;

    @NotNull(message = "Hãy chọn đối tượng áp dụng")
    private Applicability appliesTo = Applicability.ALL;
}
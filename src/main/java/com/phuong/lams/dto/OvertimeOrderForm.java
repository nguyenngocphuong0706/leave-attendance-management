package com.phuong.lams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

// Dữ liệu từ form giao việc làm thêm
@Getter
@Setter
@NoArgsConstructor
public class OvertimeOrderForm {

    @NotNull(message = "Hãy chọn nhân viên")
    private Long employeeId;

    @NotNull(message = "Hãy chọn ngày làm thêm")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    @NotNull(message = "Hãy chọn giờ bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "Hãy chọn giờ kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @NotBlank(message = "Hãy nhập lý do làm thêm")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;
}
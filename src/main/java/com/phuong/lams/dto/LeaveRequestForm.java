package com.phuong.lams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Dữ liệu từ form xin nghỉ phép. Không có ô số ngày vì server tự tính
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestForm {

    @NotNull(message = "Hãy chọn loại nghỉ")
    private Long leaveTypeId;

    // Ô ngày của trình duyệt gửi dạng năm-tháng-ngày, nên cần khai báo định dạng ISO
    @NotNull(message = "Hãy chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Hãy chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank(message = "Hãy nhập lý do nghỉ")
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;
}
package com.phuong.lams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalTime;
import java.time.ZoneId;

// Quy tắc chấm công, đọc từ khối lams.attendance trong application.yaml
@ConfigurationProperties(prefix = "lams.attendance")
public record AttendanceProperties(
        @DefaultValue("08:30") String workStart,
        @DefaultValue("17:30") String workEnd,
        @DefaultValue("15") int lateGraceMinutes,
        @DefaultValue("Asia/Ho_Chi_Minh") String zone) {

    public LocalTime gioVaoChuan() {
        return LocalTime.parse(workStart);
    }

    public LocalTime gioRaChuan() {
        return LocalTime.parse(workEnd);
    }

    public ZoneId muiGio() {
        return ZoneId.of(zone);
    }
}
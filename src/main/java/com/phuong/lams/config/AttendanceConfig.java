package com.phuong.lams.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AttendanceProperties.class)
public class AttendanceConfig {

    // Đồng hồ của ứng dụng, cố định theo múi giờ công ty. Máy chủ cloud thường chạy giờ UTC,
    // nếu dùng giờ máy trực tiếp thì chấm công sẽ lệch 7 tiếng
    @Bean
    public Clock clock(AttendanceProperties props) {
        return Clock.system(props.muiGio());
    }
}
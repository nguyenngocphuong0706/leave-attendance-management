-- Lệnh làm thêm giờ: quản lý yêu cầu, nhân viên đồng ý hoặc từ chối
CREATE TABLE overtime_orders (
                                 id            BIGSERIAL PRIMARY KEY,
                                 user_id       BIGINT       NOT NULL REFERENCES users(id),
                                 created_by    BIGINT       NOT NULL REFERENCES users(id),
                                 work_date     DATE         NOT NULL,
                                 start_time    TIME         NOT NULL,
                                 end_time      TIME         NOT NULL,
                                 reason        VARCHAR(500) NOT NULL,
                                 status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                     CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
                                 response_note VARCHAR(500),
                                 responded_at  TIMESTAMP,
                                 created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                                 CONSTRAINT ck_overtime_times CHECK (end_time > start_time),
                                 CONSTRAINT ck_overtime_max_12h CHECK (end_time - start_time <= INTERVAL '12 hours')
    );

-- Mỗi người chỉ có một lệnh còn hiệu lực cho một ngày.
-- Lệnh đã từ chối hoặc đã hủy không bị tính, nên vẫn tạo lại được lệnh mới cho ngày đó
CREATE UNIQUE INDEX uk_overtime_active_per_day
    ON overtime_orders (user_id, work_date)
    WHERE status IN ('PENDING', 'ACCEPTED');

CREATE INDEX idx_overtime_created_by ON overtime_orders(created_by);

-- Bản ghi chấm công của ngày làm thêm trỏ tới lệnh tương ứng
ALTER TABLE attendance_records
    ADD COLUMN overtime_order_id BIGINT REFERENCES overtime_orders(id);
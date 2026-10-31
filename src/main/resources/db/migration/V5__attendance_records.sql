-- Bản ghi chấm công: mỗi người mỗi ngày đúng một dòng
CREATE TABLE attendance_records (
                                    id         BIGSERIAL PRIMARY KEY,
                                    user_id    BIGINT    NOT NULL REFERENCES users(id),
                                    work_date  DATE      NOT NULL,
                                    check_in   TIMESTAMP NOT NULL,
                                    check_out  TIMESTAMP,
    -- Kết luận đi muộn, về sớm được ghi lúc chấm công theo quy tắc hiện hành
                                    late       BOOLEAN   NOT NULL DEFAULT FALSE,
                                    left_early BOOLEAN   NOT NULL DEFAULT FALSE,
    -- Một người chỉ có một bản ghi cho mỗi ngày. Đây cũng là chỉ mục tìm theo người và ngày
                                    CONSTRAINT uk_attendance_user_date UNIQUE (user_id, work_date),
    -- Giờ ra không được trước giờ vào
                                    CONSTRAINT ck_attendance_times CHECK (check_out IS NULL OR check_out >= check_in)
);

-- Báo cáo tháng sẽ lọc theo ngày làm việc
CREATE INDEX idx_attendance_work_date ON attendance_records(work_date);
-- Đơn xin nghỉ phép
CREATE TABLE leave_requests (
                                id            BIGSERIAL PRIMARY KEY,
                                user_id       BIGINT       NOT NULL REFERENCES users(id),
                                leave_type_id BIGINT       NOT NULL REFERENCES leave_types(id),
                                start_date    DATE         NOT NULL,
                                end_date      DATE         NOT NULL,
                                days          INT          NOT NULL CHECK (days > 0),
                                reason        VARCHAR(500) NOT NULL,
                                status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    -- Ba cột dưới được điền khi có người duyệt hoặc từ chối (làm ở 10.2)
                                reviewer_id   BIGINT       REFERENCES users(id),
                                review_note   VARCHAR(500),
                                reviewed_at   TIMESTAMP,
                                created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                                CONSTRAINT ck_leave_dates CHECK (end_date >= start_date)
);

-- Hai cột hay dùng để lọc: đơn của một người, đơn theo trạng thái
CREATE INDEX idx_leave_requests_user ON leave_requests(user_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
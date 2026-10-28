-- Danh mục loại nghỉ phép
CREATE TABLE leave_types (
                             id            BIGSERIAL PRIMARY KEY,
                             name          VARCHAR(100) NOT NULL UNIQUE,
                             days_per_year INT          NOT NULL CHECK (days_per_year >= 0),
                             active        BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Số ngày phép của từng người, từng loại, từng năm
CREATE TABLE leave_balances (
                                id            BIGSERIAL PRIMARY KEY,
                                user_id       BIGINT NOT NULL REFERENCES users(id),
                                leave_type_id BIGINT NOT NULL REFERENCES leave_types(id),
                                year          INT    NOT NULL,
                                total_days    INT    NOT NULL,
                                used_days     INT    NOT NULL DEFAULT 0,
    -- Mỗi người chỉ có một dòng cho mỗi loại nghỉ trong mỗi năm
                                CONSTRAINT uk_balance UNIQUE (user_id, leave_type_id, year),
    -- Không cho dùng quá số ngày được cấp, database tự chặn dù code có sai
                                CONSTRAINT ck_balance_used CHECK (used_days >= 0 AND used_days <= total_days)
);

-- Dữ liệu mẫu: các loại nghỉ
INSERT INTO leave_types (name, days_per_year) VALUES
                                                  ('Nghỉ phép năm', 12),
                                                  ('Nghỉ ốm', 10),
                                                  ('Nghỉ việc riêng', 3);

-- Cấp số dư năm hiện tại cho mọi người dùng đang có, với mọi loại nghỉ
INSERT INTO leave_balances (user_id, leave_type_id, year, total_days)
SELECT u.id, t.id, EXTRACT(YEAR FROM CURRENT_DATE)::INT, t.days_per_year
FROM users u CROSS JOIN leave_types t;
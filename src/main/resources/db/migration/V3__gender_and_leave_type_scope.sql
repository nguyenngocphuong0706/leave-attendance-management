-- Giới tính người dùng. Cho phép trống để không ép dữ liệu cũ thành giá trị đoán mò
ALTER TABLE users ADD COLUMN gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE'));

-- Dữ liệu mẫu hiện có
UPDATE users SET gender = 'MALE'   WHERE email IN ('manager@lams.com', 'an@lams.com');
UPDATE users SET gender = 'FEMALE' WHERE email IN ('binh@lams.com', 'chi@lams.com');

-- Loại nghỉ áp dụng cho ai: ALL (tất cả), MALE (chỉ nam), FEMALE (chỉ nữ)
ALTER TABLE leave_types
    ADD COLUMN applies_to VARCHAR(10) NOT NULL DEFAULT 'ALL'
        CHECK (applies_to IN ('ALL', 'MALE', 'FEMALE'));

-- Nghỉ thai sản chỉ dành cho nữ
UPDATE leave_types SET applies_to = 'FEMALE' WHERE name = 'Nghỉ thai sản';

-- Dọn số dư đã cấp sai khi chưa có quy tắc: người không thuộc đối tượng và chưa dùng ngày nào
DELETE FROM leave_balances b
    USING users u, leave_types t
WHERE b.user_id = u.id
  AND b.leave_type_id = t.id
  AND b.used_days = 0
  AND ((t.applies_to = 'FEMALE' AND u.gender IS DISTINCT FROM 'FEMALE')
   OR (t.applies_to = 'MALE' AND u.gender IS DISTINCT FROM 'MALE'));
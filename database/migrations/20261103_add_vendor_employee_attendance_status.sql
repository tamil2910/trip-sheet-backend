ALTER TABLE vendor_employee_attendance
    MODIFY COLUMN clock_in_at BIGINT NULL,
    ADD COLUMN attendance_status VARCHAR(16) NOT NULL DEFAULT 'PRESENT' AFTER attendance_date;

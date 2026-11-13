ALTER TABLE shifts
    ADD COLUMN shift_type VARCHAR(255) NULL AFTER end_time;

UPDATE shifts
SET shift_type = 'LOGIN'
WHERE shift_type IS NULL;

ALTER TABLE shifts
    MODIFY COLUMN shift_type VARCHAR(255) NOT NULL;

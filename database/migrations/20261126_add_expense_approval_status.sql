ALTER TABLE expenses
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN status_comment VARCHAR(1000) NULL;

INSERT IGNORE INTO permissions (id, name, description, module_name, created_at, updated_at, is_deleted)
VALUES
    (UUID_TO_BIN(UUID()), 'CAN_READ_EXPENSE', 'View payable expense requests', 'EXPENSE',
     UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, b'0'),
    (UUID_TO_BIN(UUID()), 'CAN_UPDATE_EXPENSE', 'Approve or reject expense requests', 'EXPENSE',
     UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000, b'0');

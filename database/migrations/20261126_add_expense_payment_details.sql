ALTER TABLE expenses
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN paid_payment_mode VARCHAR(20) NULL,
    ADD COLUMN payment_reference VARCHAR(255) NULL,
    ADD COLUMN paid_bank_account_id BINARY(16) NULL,
    ADD CONSTRAINT fk_expenses_paid_bank_account
        FOREIGN KEY (paid_bank_account_id) REFERENCES bank_accounts (id);

ALTER TABLE petty_cash_breakdowns
    ADD COLUMN paid_payment_mode VARCHAR(20) NULL,
    ADD COLUMN payment_reference VARCHAR(255) NULL,
    ADD COLUMN paid_bank_account_id BINARY(16) NULL,
    ADD CONSTRAINT fk_petty_cash_paid_bank_account
        FOREIGN KEY (paid_bank_account_id) REFERENCES bank_accounts (id);

ALTER TABLE vehicle_expense_breakdowns
    ADD COLUMN paid_payment_mode VARCHAR(20) NULL,
    ADD COLUMN payment_reference VARCHAR(255) NULL,
    ADD COLUMN paid_bank_account_id BINARY(16) NULL,
    ADD CONSTRAINT fk_vehicle_expense_paid_bank_account
        FOREIGN KEY (paid_bank_account_id) REFERENCES bank_accounts (id);

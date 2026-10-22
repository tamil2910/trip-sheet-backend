-- Detailed petty-cash and vehicle-expense rows, optionally linked to a parent expense.
CREATE TABLE IF NOT EXISTS petty_cash_breakdowns (
    id              BINARY(16)    NOT NULL,
    created_at      BIGINT        NULL,
    updated_at      BIGINT        NULL,
    deleted_at      BIGINT        NULL,
    created_by      VARCHAR(255)  NULL,
    updated_by      VARCHAR(255)  NULL,
    deleted_by      VARCHAR(255)  NULL,
    is_deleted      BIT           NULL,
    tenant_id       BINARY(16)    NOT NULL,
    expense_id      BINARY(16)    NULL,
    spent_date      DATE          NOT NULL,
    description     VARCHAR(250)  NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    paid_to         VARCHAR(200)  NULL,
    comment         TEXT          NULL,
    status          VARCHAR(20)   NOT NULL,
    payment_status  VARCHAR(20)   NOT NULL,
    status_comment  VARCHAR(1000) NULL,

    PRIMARY KEY (id),
    KEY idx_petty_cash_tenant_date (tenant_id, is_deleted, spent_date),
    KEY idx_petty_cash_expense (tenant_id, expense_id, is_deleted),
    CONSTRAINT fk_petty_cash_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_petty_cash_expense
        FOREIGN KEY (expense_id) REFERENCES expenses (id)
);

CREATE TABLE IF NOT EXISTS vehicle_expense_breakdowns (
    id               BINARY(16)    NOT NULL,
    created_at       BIGINT        NULL,
    updated_at       BIGINT        NULL,
    deleted_at       BIGINT        NULL,
    created_by       VARCHAR(255)  NULL,
    updated_by       VARCHAR(255)  NULL,
    deleted_by       VARCHAR(255)  NULL,
    is_deleted       BIT           NULL,
    tenant_id        BINARY(16)    NOT NULL,
    expense_id       BINARY(16)    NULL,
    vehicle_id       BINARY(16)    NOT NULL,
    expense_date     DATE          NOT NULL,
    description      VARCHAR(250)  NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    vendor_name      VARCHAR(200)  NULL,
    reference_number VARCHAR(100)  NULL,
    comment          TEXT          NULL,
    status           VARCHAR(20)   NOT NULL,
    payment_status   VARCHAR(20)   NOT NULL,
    status_comment   VARCHAR(1000) NULL,

    PRIMARY KEY (id),
    KEY idx_vehicle_expense_tenant_date (tenant_id, is_deleted, expense_date),
    KEY idx_vehicle_expense_expense (tenant_id, expense_id, is_deleted),
    KEY idx_vehicle_expense_vehicle (vehicle_id),
    CONSTRAINT fk_vehicle_expense_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_vehicle_expense_parent
        FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_vehicle_expense_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
);

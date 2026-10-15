-- Tenant-scoped expense setup and expense transactions.

CREATE TABLE IF NOT EXISTS expense_ledgers (
    id              BINARY(16)   NOT NULL,
    created_at      BIGINT       NULL,
    updated_at      BIGINT       NULL,
    deleted_at      BIGINT       NULL,
    created_by      VARCHAR(255) NULL,
    updated_by      VARCHAR(255) NULL,
    deleted_by      VARCHAR(255) NULL,
    is_deleted      BIT          NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT         NULL,
    is_active       BIT          DEFAULT b'1',
    tenant_id       BINARY(16)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_expense_ledger_tenant_name (tenant_id, name),
    KEY idx_expense_ledger_tenant (tenant_id, is_deleted),
    CONSTRAINT fk_expense_ledger_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS expense_categories (
    id              BINARY(16)   NOT NULL,
    created_at      BIGINT       NULL,
    updated_at      BIGINT       NULL,
    deleted_at      BIGINT       NULL,
    created_by      VARCHAR(255) NULL,
    updated_by      VARCHAR(255) NULL,
    deleted_by      VARCHAR(255) NULL,
    is_deleted      BIT          NULL,
    name            VARCHAR(150) NOT NULL,
    is_active       BIT          DEFAULT b'1',
    tenant_id       BINARY(16)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_expense_category_tenant_name (tenant_id, name),
    KEY idx_expense_category_tenant (tenant_id, is_deleted),
    CONSTRAINT fk_expense_category_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS expense_sub_categories (
    id              BINARY(16)   NOT NULL,
    created_at      BIGINT       NULL,
    updated_at      BIGINT       NULL,
    deleted_at      BIGINT       NULL,
    created_by      VARCHAR(255) NULL,
    updated_by      VARCHAR(255) NULL,
    deleted_by      VARCHAR(255) NULL,
    is_deleted      BIT          NULL,
    name            VARCHAR(150) NOT NULL,
    is_active       BIT          DEFAULT b'1',
    tenant_id       BINARY(16)   NOT NULL,
    category_id     BINARY(16)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_expense_sub_category_category_name (category_id, name),
    KEY idx_expense_sub_category_tenant (tenant_id, category_id, is_deleted),
    CONSTRAINT fk_expense_sub_category_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_expense_sub_category_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id                  BINARY(16)    NOT NULL,
    created_at          BIGINT        NULL,
    updated_at          BIGINT        NULL,
    deleted_at          BIGINT        NULL,
    created_by          VARCHAR(255)  NULL,
    updated_by          VARCHAR(255)  NULL,
    deleted_by          VARCHAR(255)  NULL,
    is_deleted          BIT           NULL,
    tenant_id           BINARY(16)    NOT NULL,
    ledger_id           BINARY(16)    NOT NULL,
    category_id         BINARY(16)    NOT NULL,
    sub_category_id     BINARY(16)    NOT NULL,
    requested_date      DATE          NOT NULL,
    payment_type        VARCHAR(30)   NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    tds_deducted        DECIMAL(12,2) NOT NULL DEFAULT 0,
    adjustments         DECIMAL(12,2) NOT NULL DEFAULT 0,
    expense_name        VARCHAR(250)  NOT NULL,
    comment             TEXT          NULL,
    payment_mode        VARCHAR(30)   NOT NULL,
    bank_account_id     BINARY(16)    NULL,
    bank_debit_date     DATE          NULL,

    PRIMARY KEY (id),
    KEY idx_expense_tenant_date (tenant_id, is_deleted, requested_date),
    KEY idx_expense_ledger (ledger_id),
    KEY idx_expense_category (category_id),
    KEY idx_expense_sub_category (sub_category_id),
    KEY idx_expense_bank_account (bank_account_id),
    CONSTRAINT fk_expense_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_expense_ledger
        FOREIGN KEY (ledger_id) REFERENCES expense_ledgers (id),
    CONSTRAINT fk_expense_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id),
    CONSTRAINT fk_expense_sub_category
        FOREIGN KEY (sub_category_id) REFERENCES expense_sub_categories (id),
    CONSTRAINT fk_expense_bank_account
        FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id)
);

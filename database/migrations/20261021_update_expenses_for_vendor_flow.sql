-- Allow expenses to be recorded against a ledger without a category.
ALTER TABLE expenses
    MODIFY COLUMN category_id BINARY(16) NULL,
    MODIFY COLUMN sub_category_id BINARY(16) NULL;

-- A ledger can optionally be assigned a category when it is created.
ALTER TABLE expense_ledgers
    ADD COLUMN category_id BINARY(16) NULL,
    ADD KEY idx_expense_ledger_category (category_id),
    ADD CONSTRAINT fk_expense_ledger_category
        FOREIGN KEY (category_id) REFERENCES expense_categories (id);

-- Proof files are stored in the configured private Railway bucket; this table stores their metadata and object keys.
CREATE TABLE IF NOT EXISTS expense_proofs (
    id              BINARY(16)    NOT NULL,
    created_at      BIGINT        NULL,
    updated_at      BIGINT        NULL,
    deleted_at      BIGINT        NULL,
    created_by      VARCHAR(255)  NULL,
    updated_by      VARCHAR(255)  NULL,
    deleted_by      VARCHAR(255)  NULL,
    is_deleted      BIT           NULL,
    expense_id      BINARY(16)    NOT NULL,
    file_key        VARCHAR(1024) NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    file_size       BIGINT        NOT NULL,

    PRIMARY KEY (id),
    KEY idx_expense_proof_expense (expense_id, is_deleted),
    CONSTRAINT fk_expense_proof_expense
        FOREIGN KEY (expense_id) REFERENCES expenses (id)
);

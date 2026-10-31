-- Optional invoice references attached to expenses.
CREATE TABLE IF NOT EXISTS expense_invoices (
    expense_id      BINARY(16)    NOT NULL,
    invoice_id      BINARY(16)    NULL,
    invoice_number  VARCHAR(100)  NULL,
    KEY idx_expense_invoice_expense (expense_id),
    KEY idx_expense_invoice_invoice (invoice_id),
    CONSTRAINT fk_expense_invoice_expense
        FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_expense_invoice_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

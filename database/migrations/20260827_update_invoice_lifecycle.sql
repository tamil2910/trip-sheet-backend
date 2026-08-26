-- Convert the legacy invoice lifecycle to the current invoice model.
-- Run this once against each existing MySQL database before deploying the code.

ALTER TABLE invoices
  MODIFY COLUMN status ENUM(
    'DRAFT', 'VERIFIED', 'APPROVED', 'ISSUED',
    'GENERATED', 'PAYMENT_RECEIVED', 'CANCELLED'
  ) NULL;

UPDATE invoices
SET status = 'GENERATED'
WHERE status IN ('DRAFT', 'VERIFIED', 'APPROVED', 'ISSUED') OR status IS NULL;

ALTER TABLE invoices
  MODIFY COLUMN status ENUM('GENERATED', 'PAYMENT_RECEIVED', 'CANCELLED') NOT NULL;

ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS purchase_order_id BINARY(16) NULL,
  ADD COLUMN IF NOT EXISTS approved_by_side ENUM('VENDOR', 'ORGANISATION') NULL,
  ADD COLUMN IF NOT EXISTS approved_by_user_id VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS approved_at BIGINT NULL,
  ADD COLUMN IF NOT EXISTS is_printed_invoice BIT NOT NULL DEFAULT b'0',
  ADD COLUMN IF NOT EXISTS is_downloaded_invoice BIT NOT NULL DEFAULT b'0';

ALTER TABLE invoices
  ADD UNIQUE INDEX IF NOT EXISTS uk_invoices_purchase_order (purchase_order_id),
  ADD CONSTRAINT fk_invoices_purchase_order
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id);

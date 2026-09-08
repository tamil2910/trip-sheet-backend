-- Add invoice generation, payment due and calendar-period timestamps (epoch milliseconds).
ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS invoice_date BIGINT NULL,
  ADD COLUMN IF NOT EXISTS due_date BIGINT NULL,
  ADD COLUMN IF NOT EXISTS invoice_period_start BIGINT NULL,
  ADD COLUMN IF NOT EXISTS invoice_period_end BIGINT NULL;

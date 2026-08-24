ALTER TABLE purchase_orders
  ADD COLUMN IF NOT EXISTS daily_allowance_charge_amount DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS daily_allowance_qty DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS daily_allowance_total DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS early_allowance_charge_amount DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS early_allowance_qty DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS early_allowance_total DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS late_allowance_charge_amount DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS late_allowance_qty DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS late_allowance_total DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS hourly_allowance_charge_amount DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS hourly_allowance_qty DECIMAL(12, 2) NULL,
  ADD COLUMN IF NOT EXISTS hourly_allowance_total DECIMAL(12, 2) NULL;

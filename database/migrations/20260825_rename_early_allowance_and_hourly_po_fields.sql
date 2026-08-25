-- Rate-card early allowance is a cutoff/end time, not a start time.
ALTER TABLE vendor_organisation_rate_cards
  RENAME COLUMN early_allowance_start_time TO early_allowance_end_time;

ALTER TABLE vendor_partner_rate_cards
  RENAME COLUMN early_allowance_start_time TO early_allowance_end_time;

-- Keep the PO's hourly allowance as a separate charge, quantity, and amount.
ALTER TABLE purchase_orders
  RENAME COLUMN hourly_allowance_charge_amount TO hourly_allowance_charge;

ALTER TABLE purchase_orders
  RENAME COLUMN hourly_allowance_total TO hourly_allowance_amount;

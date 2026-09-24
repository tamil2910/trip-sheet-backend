CREATE TABLE IF NOT EXISTS invoice_number_rules (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  tenant_id BINARY(16) NOT NULL,
  prefix VARCHAR(255) NOT NULL,
  financial_year VARCHAR(9) NOT NULL,
  suffix VARCHAR(255) NULL,
  sequence_start BIGINT NOT NULL,
  next_sequence BIGINT NOT NULL,
  next_combined_sequence BIGINT NOT NULL,
  is_default BIT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_invoice_number_rule_tenant (tenant_id),
  INDEX idx_invoice_number_rule_tenant_default (tenant_id, is_default),
  CONSTRAINT fk_invoice_number_rule_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS purchase_order_number_rules (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  vendor_id BINARY(16) NOT NULL,
  period VARCHAR(9) NOT NULL,
  suffix VARCHAR(255) NULL,
  sequence_start BIGINT NOT NULL,
  next_sequence BIGINT NOT NULL,
  is_default BIT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_po_number_rule_vendor (vendor_id),
  INDEX idx_po_number_rule_vendor_default (vendor_id, is_default),
  CONSTRAINT fk_po_number_rule_vendor FOREIGN KEY (vendor_id) REFERENCES tenants (id)
);

-- Private payable raised for each adjacent vendor delegation hop.
CREATE TABLE IF NOT EXISTS purchase_invoices (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  invoice_number VARCHAR(255) NULL,
  purchase_invoice_number VARCHAR(255) NULL,
  source_invoice_id BINARY(16) NULL,
  delegation_history_id BINARY(16) NOT NULL,
  trip_summary_id BINARY(16) NOT NULL,
  payer_vendor_id BINARY(16) NOT NULL,
  payee_vendor_id BINARY(16) NOT NULL,
  amount_payable DECIMAL(12,2) NOT NULL,
  amount_receivable DECIMAL(12,2) NOT NULL,
  earning DECIMAL(12,2) NOT NULL,
  currency_code VARCHAR(255),
  rate_card_package_name VARCHAR(255),
  notes VARCHAR(255),
  status VARCHAR(32) NOT NULL DEFAULT 'GENERATED',
  PRIMARY KEY (id),
  UNIQUE KEY uk_purchase_invoice_delegation (delegation_history_id),
  UNIQUE KEY uk_purchase_invoice_source_invoice (source_invoice_id),
  INDEX idx_purchase_invoice_payer (payer_vendor_id),
  INDEX idx_purchase_invoice_payee (payee_vendor_id),
  CONSTRAINT fk_purchase_invoice_delegation FOREIGN KEY (delegation_history_id)
    REFERENCES trip_vendor_delegation_history (id),
  CONSTRAINT fk_purchase_invoice_summary FOREIGN KEY (trip_summary_id)
    REFERENCES trip_summaries (id),
  CONSTRAINT fk_purchase_invoice_payer FOREIGN KEY (payer_vendor_id)
    REFERENCES tenants (id),
  CONSTRAINT fk_purchase_invoice_payee FOREIGN KEY (payee_vendor_id)
    REFERENCES tenants (id),
  CONSTRAINT fk_purchase_invoice_source_invoice FOREIGN KEY (source_invoice_id)
    REFERENCES invoices (id)
);

-- Vendor-to-vendor invoice workflow fields for databases that already had
-- purchase_invoices before the linked Vendor B PO was introduced.
SET @purchase_invoice_number_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'purchase_invoices'
    AND column_name = 'purchase_invoice_number'
);
SET @purchase_invoice_number_sql := IF(@purchase_invoice_number_exists = 0,
  'ALTER TABLE purchase_invoices ADD COLUMN purchase_invoice_number VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE purchase_invoice_number_statement FROM @purchase_invoice_number_sql;
EXECUTE purchase_invoice_number_statement;
DEALLOCATE PREPARE purchase_invoice_number_statement;

SET @purchase_invoice_source_invoice_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'purchase_invoices'
    AND column_name = 'source_invoice_id'
);
SET @purchase_invoice_source_invoice_sql := IF(@purchase_invoice_source_invoice_exists = 0,
  'ALTER TABLE purchase_invoices ADD COLUMN source_invoice_id BINARY(16) NULL',
  'SELECT 1'
);
PREPARE purchase_invoice_source_invoice_statement FROM @purchase_invoice_source_invoice_sql;
EXECUTE purchase_invoice_source_invoice_statement;
DEALLOCATE PREPARE purchase_invoice_source_invoice_statement;

SET @purchase_invoice_source_invoice_index_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_invoices'
    AND index_name = 'uk_purchase_invoice_source_invoice'
);
SET @purchase_invoice_source_invoice_index_sql := IF(@purchase_invoice_source_invoice_index_exists = 0,
  'ALTER TABLE purchase_invoices ADD UNIQUE KEY uk_purchase_invoice_source_invoice (source_invoice_id)',
  'SELECT 1'
);
PREPARE purchase_invoice_source_invoice_index_statement FROM @purchase_invoice_source_invoice_index_sql;
EXECUTE purchase_invoice_source_invoice_index_statement;
DEALLOCATE PREPARE purchase_invoice_source_invoice_index_statement;

-- ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS supplier_vendor_id BINARY(16) NULL;
UPDATE purchase_orders po
JOIN tenants t ON t.tenant_name = po.supplier_name AND t.tenant_type = 'VENDOR'
SET po.supplier_vendor_id = t.id
WHERE po.supplier_vendor_id IS NULL;

-- A trip summary has one organisation PO plus one PO for each delegation hop.
-- Older versions mapped this relation as one-to-one, which made the first PO
-- consume the unique trip_summary_id and blocked the Vendor B PO.
SET @po_trip_summary_index_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'purchase_orders'
    AND index_name = 'idx_purchase_orders_trip_summary'
);
SET @po_trip_summary_index_sql := IF(@po_trip_summary_index_exists = 0,
  'ALTER TABLE purchase_orders ADD INDEX idx_purchase_orders_trip_summary (trip_summary_id)',
  'SELECT 1'
);
PREPARE po_trip_summary_index_statement FROM @po_trip_summary_index_sql;
EXECUTE po_trip_summary_index_statement;
DEALLOCATE PREPARE po_trip_summary_index_statement;

SELECT index_name INTO @legacy_po_trip_summary_unique_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'purchase_orders'
  AND column_name = 'trip_summary_id' AND non_unique = 0 AND index_name <> 'PRIMARY'
LIMIT 1;
SET @legacy_po_trip_summary_unique_sql := IF(@legacy_po_trip_summary_unique_index IS NULL,
  'SELECT 1',
  CONCAT('ALTER TABLE purchase_orders DROP INDEX `', @legacy_po_trip_summary_unique_index, '`')
);
PREPARE legacy_po_trip_summary_unique_statement FROM @legacy_po_trip_summary_unique_sql;
EXECUTE legacy_po_trip_summary_unique_statement;
DEALLOCATE PREPARE legacy_po_trip_summary_unique_statement;

-- Add the PO-equivalent snapshot fields to databases created by the first
-- PurchaseInvoice version (safe to run repeatedly on MySQL 8).
-- ALTER TABLE purchase_invoices
--   ADD COLUMN IF NOT EXISTS order_number VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS document_type VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS document_date BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS due_date BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS billing_period_start BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS billing_period_end BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS bill_to_name VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS bill_to_code VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS bill_to_gstin VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS bill_to_address VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS supplier_name VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS supplier_phone VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS supplier_address VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS line_item_count INT NULL,
--   ADD COLUMN IF NOT EXISTS line_items_snapshot LONGTEXT NULL,
--   ADD COLUMN IF NOT EXISTS garage_start_time BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS garage_end_time BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_start_time BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_start_km_odo BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_start_km_odo_image BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_end_time BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_end_km_odo BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_end_km_odo_image BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_duration BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_distance BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_extra_km_odo BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_extra_km BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_extra_hr BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_startgpskm BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_endgpskm BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_gps_duration BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS trip_gps_distance BIGINT NULL,
--   ADD COLUMN IF NOT EXISTS dispatch_lat DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS dispatch_lng DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS arrived_lat DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS arrived_lng DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS trip_start_lat DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS trip_start_lng DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS trip_end_lat DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS trip_end_lng DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS garage_end_lat DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS garage_end_lng DOUBLE NULL,
--   ADD COLUMN IF NOT EXISTS trip_calculation_field_name VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS extra_hr_calculation_field_name VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS extra_km_calculation_field_name VARCHAR(255) NULL,
--   ADD COLUMN IF NOT EXISTS base_fare_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS base_fare_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS base_fare_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_km_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_km_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_km_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_hr_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_hr_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS extra_hr_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS daily_allowance_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS daily_allowance_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS daily_allowance_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS early_allowance_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS early_allowance_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS early_allowance_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS late_allowance_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS late_allowance_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS late_allowance_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS hourly_allowance_charge DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS hourly_allowance_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS hourly_allowance_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS toll_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS toll_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS toll_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS parking_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS parking_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS parking_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS other_charge_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS other_qty DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS other_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS taxable_sub_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS gst_percentage DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS gst_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS cgst_percentage DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS cgst_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS sgst_percentage DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS sgst_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS igst_percentage DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS igst_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS taxable_total_with_gst DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS non_taxable_total DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS round_off_amount DECIMAL(12,2) NULL,
--   ADD COLUMN IF NOT EXISTS total_amount DECIMAL(12,2) NULL;
-- ALTER TABLE purchase_invoices
--   ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'GENERATED';

SET @trip_arrived_time_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'trip_summaries'
    AND column_name = 'trip_arrived_time'
);
SET @trip_arrived_time_sql := IF(
  @trip_arrived_time_exists = 0,
  'ALTER TABLE trip_summaries ADD COLUMN trip_arrived_time BIGINT NULL',
  'SELECT 1'
);
PREPARE trip_arrived_time_statement FROM @trip_arrived_time_sql;
EXECUTE trip_arrived_time_statement;
DEALLOCATE PREPARE trip_arrived_time_statement;

SET @trip_airport_transfer_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'trips'
    AND column_name = 'airport_transfer_type'
);
SET @trip_airport_transfer_type_sql := IF(
  @trip_airport_transfer_type_exists = 0,
  'ALTER TABLE trips ADD COLUMN airport_transfer_type VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE trip_airport_transfer_type_statement FROM @trip_airport_transfer_type_sql;
EXECUTE trip_airport_transfer_type_statement;
DEALLOCATE PREPARE trip_airport_transfer_type_statement;

SET @trip_summary_id_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'trips'
    AND column_name = 'trip_summary_id'
);
SET @trip_summary_id_sql := IF(
  @trip_summary_id_exists = 0,
  'ALTER TABLE trips ADD COLUMN trip_summary_id BINARY(16) NULL',
  'SELECT 1'
);
PREPARE trip_summary_id_statement FROM @trip_summary_id_sql;
EXECUTE trip_summary_id_statement;
DEALLOCATE PREPARE trip_summary_id_statement;

UPDATE trips trip
JOIN trip_summaries summary ON summary.trip_id = trip.id
SET trip.trip_summary_id = summary.id
WHERE trip.trip_summary_id IS NULL;

-- Keep legacy invoice databases compatible with the current invoice lifecycle.
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
  -- ADD COLUMN IF NOT EXISTS purchase_order_id BINARY(16) NULL,
  -- ADD COLUMN IF NOT EXISTS approved_by_side ENUM('VENDOR', 'ORGANISATION') NULL,
  -- ADD COLUMN IF NOT EXISTS approved_by_user_id VARCHAR(255) NULL,
  -- ADD COLUMN IF NOT EXISTS approved_at BIGINT NULL,
  -- ADD COLUMN IF NOT EXISTS is_printed_invoice BIT NOT NULL DEFAULT b'0',
  -- ADD COLUMN IF NOT EXISTS is_downloaded_invoice BIT NOT NULL DEFAULT b'0';

-- CREATE INDEX IF NOT EXISTS idx_invoices_purchase_order ON invoices (purchase_order_id);

SET @legacy_invoice_po_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'invoices'
    AND index_name = 'uk_invoices_purchase_order'
);
SET @legacy_invoice_po_index_sql := IF(
  @legacy_invoice_po_index_exists = 1,
  'ALTER TABLE invoices DROP INDEX uk_invoices_purchase_order',
  'SELECT 1'
);
PREPARE legacy_invoice_po_index_statement FROM @legacy_invoice_po_index_sql;
EXECUTE legacy_invoice_po_index_statement;
DEALLOCATE PREPARE legacy_invoice_po_index_statement;

-- Taxes selected for a vendor-to-vendor partnership.
CREATE TABLE IF NOT EXISTS vendor_partner_taxes (
  vendor_partner_id BINARY(16) NOT NULL,
  tax_id BINARY(16) NOT NULL,
  PRIMARY KEY (vendor_partner_id, tax_id),
  INDEX idx_vendor_partner_taxes_tax (tax_id),
  CONSTRAINT fk_vendor_partner_taxes_partner FOREIGN KEY (vendor_partner_id)
    REFERENCES vendor_partners (id),
  CONSTRAINT fk_vendor_partner_taxes_tax FOREIGN KEY (tax_id)
    REFERENCES taxes (id)
);

-- Older deployments created this join table with a BaseModel-style `id`
-- column. A @ManyToMany join table has no entity id, so remove that legacy
-- column and make the relationship columns its key.
SET @vendor_partner_tax_id_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vendor_partner_taxes'
    AND column_name = 'id'
);
SET @vendor_partner_tax_id_sql := IF(
  @vendor_partner_tax_id_exists = 1,
  'ALTER TABLE vendor_partner_taxes DROP PRIMARY KEY, DROP COLUMN id, ADD PRIMARY KEY (vendor_partner_id, tax_id)',
  'SELECT 1'
);
PREPARE vendor_partner_tax_id_statement FROM @vendor_partner_tax_id_sql;
EXECUTE vendor_partner_tax_id_statement;
DEALLOCATE PREPARE vendor_partner_tax_id_statement;

SET @vendor_partner_tax_tenant_id_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vendor_partner_taxes'
    AND column_name = 'tenant_id'
);
SET @vendor_partner_tax_tenant_id_fk := (
  SELECT constraint_name
  FROM information_schema.key_column_usage
  WHERE table_schema = DATABASE()
    AND table_name = 'vendor_partner_taxes'
    AND column_name = 'tenant_id'
    AND referenced_table_name IS NOT NULL
  LIMIT 1
);
SET @vendor_partner_tax_tenant_id_sql := IF(
  @vendor_partner_tax_tenant_id_exists = 1,
  CONCAT(
    'ALTER TABLE vendor_partner_taxes ',
    IF(@vendor_partner_tax_tenant_id_fk IS NULL, '',
      CONCAT('DROP FOREIGN KEY `', @vendor_partner_tax_tenant_id_fk, '`, ')),
    'DROP COLUMN tenant_id'
  ),
  'SELECT 1'
);
PREPARE vendor_partner_tax_tenant_id_statement FROM @vendor_partner_tax_tenant_id_sql;
EXECUTE vendor_partner_tax_tenant_id_statement;
DEALLOCATE PREPARE vendor_partner_tax_tenant_id_statement;

CREATE TABLE IF NOT EXISTS credit_debit_notes (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  note_number VARCHAR(255) NOT NULL,
  note_date BIGINT NOT NULL,
  apply_to VARCHAR(32) NOT NULL,
  note_type VARCHAR(255) NOT NULL,
  financial_year VARCHAR(4) NOT NULL,
  sequence_number INT NOT NULL,
  organisation_id BINARY(16) NULL,
  vendor_partner_id BINARY(16) NULL,
  is_non_taxable BIT NOT NULL,
  tax_amount DECIMAL(12,2),
  taxable_sub_total DECIMAL(12,2) NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  remaining_amount DECIMAL(12,2) NOT NULL,
  attachment_url VARCHAR(255),
  comments LONGTEXT,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credit_debit_note_number (note_number),
  UNIQUE KEY uk_credit_debit_note_sequence (organisation_id, note_type, financial_year, sequence_number),
  INDEX idx_credit_debit_note_organisation (organisation_id),
  INDEX idx_credit_debit_note_vendor_partner (vendor_partner_id),
  CONSTRAINT fk_credit_debit_note_organisation FOREIGN KEY (organisation_id) REFERENCES tenants (id),
  CONSTRAINT fk_credit_debit_note_vendor_partner FOREIGN KEY (vendor_partner_id) REFERENCES vendor_partners (id)
);

-- Repair legacy databases that predate the explicit note target.
SET @credit_debit_note_apply_to_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'credit_debit_notes'
    AND column_name = 'apply_to'
);
SET @credit_debit_note_apply_to_sql := IF(@credit_debit_note_apply_to_exists = 0,
  'ALTER TABLE credit_debit_notes ADD COLUMN apply_to VARCHAR(32) NULL AFTER note_date',
  'SELECT 1'
);
PREPARE credit_debit_note_apply_to_statement FROM @credit_debit_note_apply_to_sql;
EXECUTE credit_debit_note_apply_to_statement;
DEALLOCATE PREPARE credit_debit_note_apply_to_statement;

UPDATE credit_debit_notes
SET apply_to = CASE
  WHEN organisation_id IS NOT NULL THEN 'ORGANISATION'
  WHEN vendor_partner_id IS NOT NULL THEN 'VENDOR_PARTNER'
  ELSE apply_to
END
WHERE apply_to IS NULL;

-- Repair legacy databases that created either owner relation as NOT NULL.
ALTER TABLE credit_debit_notes
  MODIFY COLUMN organisation_id BINARY(16) NULL,
  MODIFY COLUMN vendor_partner_id BINARY(16) NULL;

CREATE TABLE IF NOT EXISTS credit_debit_note_taxes (
  credit_debit_note_id BINARY(16) NOT NULL,
  tax_id BINARY(16) NOT NULL,
  PRIMARY KEY (credit_debit_note_id, tax_id),
  INDEX idx_credit_debit_note_taxes_tax (tax_id),
  CONSTRAINT fk_credit_debit_note_taxes_note FOREIGN KEY (credit_debit_note_id) REFERENCES credit_debit_notes (id),
  CONSTRAINT fk_credit_debit_note_taxes_tax FOREIGN KEY (tax_id) REFERENCES taxes (id)
);

CREATE TABLE IF NOT EXISTS credit_debit_note_invoice_applications (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  credit_debit_note_id BINARY(16) NOT NULL,
  invoice_id BINARY(16) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_cd_note_invoice_application_note (credit_debit_note_id),
  INDEX idx_cd_note_invoice_application_invoice (invoice_id),
  CONSTRAINT fk_cd_note_invoice_application_note FOREIGN KEY (credit_debit_note_id) REFERENCES credit_debit_notes (id),
  CONSTRAINT fk_cd_note_invoice_application_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

CREATE TABLE IF NOT EXISTS credit_debit_note_purchase_invoice_applications (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  credit_debit_note_id BINARY(16) NOT NULL,
  purchase_invoice_id BINARY(16) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_cd_note_purchase_application_note (credit_debit_note_id),
  INDEX idx_cd_note_purchase_application_invoice (purchase_invoice_id),
  CONSTRAINT fk_cd_note_purchase_application_note FOREIGN KEY (credit_debit_note_id) REFERENCES credit_debit_notes (id),
  CONSTRAINT fk_cd_note_purchase_application_invoice FOREIGN KEY (purchase_invoice_id) REFERENCES purchase_invoices (id)
);

-- ALTER TABLE invoices
--   ADD COLUMN IF NOT EXISTS credit_debit_note_applied_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
--   ADD COLUMN IF NOT EXISTS current_payable_amount DECIMAL(12,2) NULL;

-- ALTER TABLE purchase_invoices
--   ADD COLUMN IF NOT EXISTS credit_debit_note_applied_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
--   ADD COLUMN IF NOT EXISTS current_payable_amount DECIMAL(12,2) NULL;

CREATE TABLE IF NOT EXISTS receipt_invoice_applications (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  receipt_id BINARY(16) NOT NULL,
  invoice_id BINARY(16) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_receipt_invoice_application_receipt (receipt_id),
  INDEX idx_receipt_invoice_application_invoice (invoice_id),
  CONSTRAINT fk_receipt_invoice_application_receipt FOREIGN KEY (receipt_id) REFERENCES receipts (id),
  CONSTRAINT fk_receipt_invoice_application_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
);

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
  PRIMARY KEY (id),
  UNIQUE KEY uk_purchase_invoice_delegation (delegation_history_id),
  INDEX idx_purchase_invoice_payer (payer_vendor_id),
  INDEX idx_purchase_invoice_payee (payee_vendor_id),
  CONSTRAINT fk_purchase_invoice_delegation FOREIGN KEY (delegation_history_id)
    REFERENCES trip_vendor_delegation_history (id),
  CONSTRAINT fk_purchase_invoice_summary FOREIGN KEY (trip_summary_id)
    REFERENCES trip_summaries (id),
  CONSTRAINT fk_purchase_invoice_payer FOREIGN KEY (payer_vendor_id)
    REFERENCES tenants (id),
  CONSTRAINT fk_purchase_invoice_payee FOREIGN KEY (payee_vendor_id)
    REFERENCES tenants (id)
);

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

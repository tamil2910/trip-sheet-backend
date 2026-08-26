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

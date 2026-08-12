-- Apply once to MySQL before deploying this billing redesign.
-- Existing trip_billing_* data is retired. Back it up first if historical allocation data is needed.

DROP PROCEDURE IF EXISTS drop_fk_for_column;
DELIMITER //
CREATE PROCEDURE drop_fk_for_column(IN table_name_arg VARCHAR(64), IN column_name_arg VARCHAR(64))
BEGIN
  DECLARE fk_name VARCHAR(64);
  SELECT constraint_name INTO fk_name
  FROM information_schema.key_column_usage
  WHERE table_schema = DATABASE()
    AND table_name = table_name_arg
    AND column_name = column_name_arg
    AND referenced_table_name IS NOT NULL
  LIMIT 1;
  IF fk_name IS NOT NULL THEN
    SET @statement = CONCAT('ALTER TABLE `', table_name_arg, '` DROP FOREIGN KEY `', fk_name, '`');
    PREPARE stmt FROM @statement;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

CALL drop_fk_for_column('purchase_orders', 'allocation_id');
ALTER TABLE purchase_orders DROP COLUMN IF EXISTS allocation_id;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS status VARCHAR(255) NULL;

CALL drop_fk_for_column('invoice_lines', 'allocation_id');
ALTER TABLE invoice_lines DROP COLUMN IF EXISTS allocation_id;

CREATE TABLE IF NOT EXISTS purchase_order_allocations (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  purchase_order_id BINARY(16) NOT NULL,
  custom_field_id BINARY(16) NULL,
  allocation_key VARCHAR(255) NOT NULL,
  share_percent DECIMAL(10,2) NOT NULL,
  share_amount DECIMAL(19,4) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_po_allocation_po (purchase_order_id),
  INDEX idx_po_allocation_custom_field (custom_field_id),
  CONSTRAINT fk_po_allocation_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
  CONSTRAINT fk_po_allocation_custom_field FOREIGN KEY (custom_field_id) REFERENCES custom_fields (id)
);

ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS allocation_id BINARY(16) NULL;
CALL drop_fk_for_column('invoice_lines', 'allocation_id');
ALTER TABLE invoice_lines ADD CONSTRAINT fk_invoice_line_po_allocation
  FOREIGN KEY (allocation_id) REFERENCES purchase_order_allocations (id);

DROP TABLE IF EXISTS trip_billing_allocations;
DROP TABLE IF EXISTS trip_billing_rules;
DROP PROCEDURE IF EXISTS drop_fk_for_column;

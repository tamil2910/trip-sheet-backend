ALTER TABLE purchase_order_number_rules
  ADD COLUMN IF NOT EXISTS next_combined_sequence BIGINT NULL;

UPDATE purchase_order_number_rules
SET next_combined_sequence = sequence_start
WHERE next_combined_sequence IS NULL;

ALTER TABLE purchase_order_number_rules
  MODIFY COLUMN next_combined_sequence BIGINT NOT NULL;

ALTER TABLE purchase_orders
  ADD COLUMN IF NOT EXISTS combined_purchase_order_id BINARY(16) NULL;

CREATE INDEX idx_purchase_order_combined ON purchase_orders (combined_purchase_order_id);

ALTER TABLE purchase_orders
  ADD CONSTRAINT fk_purchase_order_combined
  FOREIGN KEY (combined_purchase_order_id) REFERENCES purchase_orders (id);

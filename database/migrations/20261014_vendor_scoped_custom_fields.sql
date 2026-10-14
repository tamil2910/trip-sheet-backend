-- Allow vendors to define private custom fields for linked organisations and
-- associate customers. Existing rows remain organisation-owned (NULL vendor scope).
ALTER TABLE custom_fields
  ADD COLUMN created_by_vendor_id BINARY(16) NULL,
  ADD COLUMN owner_vendor_partner_id BINARY(16) NULL,
  ADD INDEX idx_custom_fields_created_by_vendor (created_by_vendor_id),
  ADD INDEX idx_custom_fields_owner_vendor_partner (owner_vendor_partner_id),
  ADD CONSTRAINT fk_custom_fields_created_by_vendor
    FOREIGN KEY (created_by_vendor_id) REFERENCES tenants (id),
  ADD CONSTRAINT fk_custom_fields_owner_vendor_partner
    FOREIGN KEY (owner_vendor_partner_id) REFERENCES vendor_partners (id);

-- Remove the previous unique key on (tenant_id, name): different vendors need
-- to be able to define private fields with the same name for the same client.
SET @custom_fields_old_unique_index = (
  SELECT s.INDEX_NAME
  FROM information_schema.STATISTICS s
  WHERE s.TABLE_SCHEMA = DATABASE()
    AND s.TABLE_NAME = 'custom_fields'
    AND s.NON_UNIQUE = 0
    AND s.INDEX_NAME <> 'PRIMARY'
  GROUP BY s.INDEX_NAME
  HAVING GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX SEPARATOR ',') = 'tenant_id,name'
  LIMIT 1
);
SET @custom_fields_drop_sql = IF(
  @custom_fields_old_unique_index IS NULL,
  'SELECT 1',
  CONCAT('ALTER TABLE custom_fields DROP INDEX `', @custom_fields_old_unique_index, '`')
);
PREPARE custom_fields_drop_stmt FROM @custom_fields_drop_sql;
EXECUTE custom_fields_drop_stmt;
DEALLOCATE PREPARE custom_fields_drop_stmt;

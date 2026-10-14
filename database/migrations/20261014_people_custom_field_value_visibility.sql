-- Track whether organisation-authored passenger values can be seen by vendors,
-- and keep each vendor's value separate from the organisation's value.
ALTER TABLE people_tenant_custom_field_values
  ADD COLUMN is_visible_to_vendor BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN created_by_vendor_id BINARY(16) NULL,
  ADD INDEX idx_people_cf_values_created_by_vendor (created_by_vendor_id),
  ADD CONSTRAINT fk_people_cf_values_created_by_vendor
    FOREIGN KEY (created_by_vendor_id) REFERENCES tenants (id);

-- Replace the old one-value-per-person-and-field key so a vendor can add its
-- own value without overwriting an organisation-authored value.
SET @people_cf_old_unique_index = (
  SELECT s.INDEX_NAME
  FROM information_schema.STATISTICS s
  WHERE s.TABLE_SCHEMA = DATABASE()
    AND s.TABLE_NAME = 'people_tenant_custom_field_values'
    AND s.NON_UNIQUE = 0
    AND s.INDEX_NAME <> 'PRIMARY'
  GROUP BY s.INDEX_NAME
  HAVING GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX SEPARATOR ',') = 'people_tenant_id,custom_field_id'
  LIMIT 1
);
SET @people_cf_drop_sql = IF(
  @people_cf_old_unique_index IS NULL,
  'SELECT 1',
  CONCAT('ALTER TABLE people_tenant_custom_field_values DROP INDEX `', @people_cf_old_unique_index, '`')
);
PREPARE people_cf_drop_stmt FROM @people_cf_drop_sql;
EXECUTE people_cf_drop_stmt;
DEALLOCATE PREPARE people_cf_drop_stmt;

CREATE TABLE IF NOT EXISTS labels (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  name VARCHAR(255) NOT NULL,
  color VARCHAR(255) NULL,
  tenant_id BINARY(16) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_label_tenant_deleted_name (tenant_id, is_deleted, name),
  CONSTRAINT fk_label_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

ALTER TABLE trips
  ADD COLUMN IF NOT EXISTS label_id BINARY(16) NULL;
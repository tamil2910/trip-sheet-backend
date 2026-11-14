CREATE TABLE IF NOT EXISTS vehicle_service_item_categories (
  id BINARY(16) NOT NULL PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  name VARCHAR(180) NOT NULL,
  normalized_name VARCHAR(180) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_vehicle_service_item_category_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  CONSTRAINT uq_vehicle_service_item_category_tenant_normalized_name UNIQUE (tenant_id, normalized_name)
);

CREATE TABLE IF NOT EXISTS vehicle_service_expense_items (
  id BINARY(16) NOT NULL PRIMARY KEY,
  vehicle_service_id BINARY(16) NOT NULL,
  category_id BINARY(16) NULL,
  item_name VARCHAR(180) NOT NULL,
  item_type VARCHAR(10) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_value DECIMAL(12,2) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_vehicle_service_expense_item_service FOREIGN KEY (vehicle_service_id) REFERENCES vehicle_services(id),
  CONSTRAINT fk_vehicle_service_expense_item_category FOREIGN KEY (category_id) REFERENCES vehicle_service_item_categories(id),
  INDEX idx_vehicle_service_expense_item_service (vehicle_service_id)
);

CREATE TABLE IF NOT EXISTS vehicle_service_invoices (
  id BINARY(16) NOT NULL PRIMARY KEY,
  vehicle_service_id BINARY(16) NOT NULL,
  file_key VARCHAR(1024) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_vehicle_service_invoice_service FOREIGN KEY (vehicle_service_id) REFERENCES vehicle_services(id),
  INDEX idx_vehicle_service_invoice_service (vehicle_service_id)
);

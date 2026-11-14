CREATE TABLE IF NOT EXISTS authorized_service_centers (
  id BINARY(16) NOT NULL PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  name VARCHAR(180) NOT NULL,
  contact_person_name VARCHAR(160) NOT NULL,
  phone_number VARCHAR(40) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_authorized_service_centers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  CONSTRAINT uq_authorized_service_centers_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS vehicle_services (
  id BINARY(16) NOT NULL PRIMARY KEY,
  tenant_id BINARY(16) NOT NULL,
  vehicle_id BINARY(16) NOT NULL,
  service_type VARCHAR(40) NOT NULL,
  authorized_service_center_id BINARY(16) NULL,
  center_name VARCHAR(180) NULL,
  calling_person_name VARCHAR(160) NULL,
  phone_number VARCHAR(40) NULL,
  dispatch_center_id BINARY(16) NULL,
  request_date BIGINT NOT NULL,
  estimated_completion_date BIGINT NULL,
  taken_in_for VARCHAR(500) NOT NULL,
  odometer_km INT NOT NULL,
  request_notes TEXT NULL,
  estimated_amount DECIMAL(12,2) NULL,
  paid_amount DECIMAL(12,2) NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_vehicle_services_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  CONSTRAINT fk_vehicle_services_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_vehicle_services_center FOREIGN KEY (authorized_service_center_id) REFERENCES authorized_service_centers(id),
  CONSTRAINT fk_vehicle_services_dispatch_center FOREIGN KEY (dispatch_center_id) REFERENCES dispatch_centers(id),
  INDEX idx_vehicle_services_tenant_date (tenant_id, request_date)
);

INSERT IGNORE INTO permissions (id, name, description, module_name, created_at, updated_at, is_deleted)
VALUES
(UUID_TO_BIN(UUID()), 'CAN_CREATE_AUTHORIZEDSERVICECENTER', 'Create authorized service centers', 'AUTHORIZED_SERVICE_CENTER', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_READ_AUTHORIZEDSERVICECENTER', 'View authorized service centers', 'AUTHORIZED_SERVICE_CENTER', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_UPDATE_AUTHORIZEDSERVICECENTER', 'Update authorized service centers', 'AUTHORIZED_SERVICE_CENTER', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_DELETE_AUTHORIZEDSERVICECENTER', 'Delete authorized service centers', 'AUTHORIZED_SERVICE_CENTER', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_CREATE_VEHICLESERVICE', 'Create vehicle service records', 'VEHICLE_SERVICE', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_READ_VEHICLESERVICE', 'View vehicle service records', 'VEHICLE_SERVICE', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_UPDATE_VEHICLESERVICE', 'Update vehicle service records', 'VEHICLE_SERVICE', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0'),
(UUID_TO_BIN(UUID()), 'CAN_DELETE_VEHICLESERVICE', 'Delete vehicle service records', 'VEHICLE_SERVICE', UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3))*1000, b'0');

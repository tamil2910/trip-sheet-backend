CREATE TABLE IF NOT EXISTS vehicle_service_progress_updates (
  id BINARY(16) NOT NULL PRIMARY KEY,
  vehicle_service_id BINARY(16) NOT NULL,
  description VARCHAR(4000) NOT NULL,
  photo_key VARCHAR(1024) NULL,
  updated_by_name VARCHAR(255) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_vehicle_service_progress_update_service FOREIGN KEY (vehicle_service_id) REFERENCES vehicle_services(id),
  INDEX idx_vehicle_service_progress_update_service_created (vehicle_service_id, created_at)
);

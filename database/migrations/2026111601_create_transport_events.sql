CREATE TABLE IF NOT EXISTS transport_events (
  id BINARY(16) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  venue VARCHAR(500) NOT NULL,
  city VARCHAR(255) NULL,
  description VARCHAR(4000) NULL,
  starts_at BIGINT NOT NULL,
  ends_at BIGINT NOT NULL,
  link_valid_until BIGINT NOT NULL,
  request_audience VARCHAR(32) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  tenant_id BINARY(16) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_transport_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
  INDEX idx_transport_events_tenant_start (tenant_id, starts_at),
  INDEX idx_transport_events_tenant_deleted (tenant_id, is_deleted)
);

CREATE TABLE IF NOT EXISTS transport_event_vehicle_types (
  event_id BINARY(16) NOT NULL,
  vehicle_type_id BINARY(16) NOT NULL,
  PRIMARY KEY (event_id, vehicle_type_id),
  CONSTRAINT fk_event_vehicle_types_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  CONSTRAINT fk_event_vehicle_types_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id),
  INDEX idx_event_vehicle_types_vehicle_type (vehicle_type_id)
);

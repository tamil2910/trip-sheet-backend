CREATE TABLE IF NOT EXISTS vendor_partner_driver_vehicles (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  primary_vendor_id BINARY(16) NOT NULL,
  partner_vendor_id BINARY(16) NOT NULL,
  type VARCHAR(16) NOT NULL,
  driver_name VARCHAR(255) NULL,
  phone VARCHAR(255) NULL,
  email VARCHAR(255) NULL,
  driver_id BINARY(16) NULL,
  vehicle_number VARCHAR(255) NULL,
  vehicle_type_id BINARY(16) NULL,
  PRIMARY KEY (id),
  INDEX idx_vpdv_primary_partner (primary_vendor_id, partner_vendor_id),
  UNIQUE KEY uq_vpdv_primary_partner_phone (primary_vendor_id, partner_vendor_id, phone),
  UNIQUE KEY uq_vpdv_primary_partner_email (primary_vendor_id, partner_vendor_id, email),
  UNIQUE KEY uq_vpdv_primary_partner_vehicle_number (primary_vendor_id, partner_vendor_id, vehicle_number),
  CONSTRAINT fk_vpdv_primary_vendor FOREIGN KEY (primary_vendor_id)
    REFERENCES tenants (id),
  CONSTRAINT fk_vpdv_partner_vendor FOREIGN KEY (partner_vendor_id)
    REFERENCES tenants (id),
  CONSTRAINT fk_vpdv_driver FOREIGN KEY (driver_id)
    REFERENCES drivers (id),
  CONSTRAINT fk_vpdv_vehicle_type FOREIGN KEY (vehicle_type_id)
    REFERENCES vehicle_types (id)
);
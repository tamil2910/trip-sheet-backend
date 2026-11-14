ALTER TABLE vehicle_services
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER service_type;

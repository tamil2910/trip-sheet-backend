-- Adds a vendor client for trips billed to an associate customer.
ALTER TABLE trips
  MODIFY COLUMN organisation_id BINARY(16) NULL,
  ADD COLUMN client_vendor_id BINARY(16) NULL,
  ADD INDEX idx_trip_client_vendor_id (client_vendor_id),
  ADD CONSTRAINT fk_trip_client_vendor
    FOREIGN KEY (client_vendor_id) REFERENCES tenants (id);

ALTER TABLE transport_route_drafts
  ADD COLUMN vendor_id BINARY(16) NULL,
  ADD CONSTRAINT fk_transport_route_drafts_vendor
    FOREIGN KEY (vendor_id) REFERENCES tenants(id),
  ADD INDEX idx_route_drafts_vendor (vendor_id);

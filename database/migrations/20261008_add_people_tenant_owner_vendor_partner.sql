-- Associates passenger records with the vendor-partner relationship used as their client.
ALTER TABLE people_tenant
  ADD COLUMN owner_vendor_partner_id BINARY(16) NULL,
  ADD INDEX idx_people_tenant_owner_vendor_partner_id (owner_vendor_partner_id),
  ADD CONSTRAINT fk_people_tenant_owner_vendor_partner
    FOREIGN KEY (owner_vendor_partner_id) REFERENCES vendor_partners (id);

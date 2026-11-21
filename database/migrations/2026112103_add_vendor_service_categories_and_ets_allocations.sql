CREATE TABLE vendor_partner_service_categories (
  vendor_partner_id BINARY(16) NOT NULL,
  service_category VARCHAR(16) NOT NULL,
  PRIMARY KEY (vendor_partner_id, service_category),
  CONSTRAINT fk_vpsc_vendor_partner FOREIGN KEY (vendor_partner_id)
    REFERENCES vendor_partners(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO vendor_partner_service_categories (vendor_partner_id, service_category)
SELECT id, 'RAC' FROM vendor_partners;
INSERT INTO vendor_partner_service_categories (vendor_partner_id, service_category)
SELECT id, 'ETS' FROM vendor_partners;
INSERT INTO vendor_partner_service_categories (vendor_partner_id, service_category)
SELECT id, 'EVENTS' FROM vendor_partners;

ALTER TABLE organisation_settings
  ADD COLUMN auto_ets_trip_assignment_enabled BIT(1) NOT NULL DEFAULT b'0';

CREATE TABLE organisation_vendor_allocations (
  id BINARY(16) NOT NULL PRIMARY KEY,
  organisation_settings_id BINARY(16) NOT NULL,
  vendor_organisation_id BINARY(16) NOT NULL,
  allocation_percentage DECIMAL(5,2) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_ova_settings FOREIGN KEY (organisation_settings_id)
    REFERENCES organisation_settings(id) ON DELETE CASCADE,
  CONSTRAINT fk_ova_vendor_organisation FOREIGN KEY (vendor_organisation_id)
    REFERENCES vendor_organisations(id),
  CONSTRAINT uq_ova_settings_vendor_link UNIQUE (organisation_settings_id, vendor_organisation_id),
  INDEX idx_ova_vendor_link (vendor_organisation_id)
) ENGINE=InnoDB;

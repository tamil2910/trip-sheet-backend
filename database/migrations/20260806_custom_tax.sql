-- Apply this migration once to an existing MySQL database before deploying.
DROP TABLE IF EXISTS vendor_partner_taxes;
DROP TABLE IF EXISTS vendor_organisation_taxes;

ALTER TABLE taxes DROP COLUMN tenant_id;

CREATE TABLE custom_tax (
  id BINARY(16) NOT NULL,
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT,
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  deleted_by VARCHAR(255),
  is_deleted BIT,
  custom_tax_name VARCHAR(255) NOT NULL,
  is_active BIT,
  tenant_id BINARY(16) NOT NULL,
  tax_id BINARY(16) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_custom_tax_tenant_tax UNIQUE (tenant_id, tax_id),
  CONSTRAINT fk_custom_tax_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_custom_tax_tax FOREIGN KEY (tax_id) REFERENCES taxes (id)
);

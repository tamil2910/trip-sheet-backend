-- Stores the taxes applicable to each vendor-organisation relationship.
CREATE TABLE vendor_organisation_taxes (
  vendor_organisation_id BINARY(16) NOT NULL,
  tax_id BINARY(16) NOT NULL,
  PRIMARY KEY (vendor_organisation_id, tax_id),
  CONSTRAINT fk_vendor_organisation_taxes_vendor_organisation
    FOREIGN KEY (vendor_organisation_id) REFERENCES vendor_organisations (id),
  CONSTRAINT fk_vendor_organisation_taxes_tax
    FOREIGN KEY (tax_id) REFERENCES taxes (id)
);

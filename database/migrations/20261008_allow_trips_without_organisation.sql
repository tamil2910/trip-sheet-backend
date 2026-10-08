-- Associate-customer trips use client_vendor_id and leave organisation_id null.
ALTER TABLE trips
  MODIFY COLUMN organisation_id BINARY(16) NULL;

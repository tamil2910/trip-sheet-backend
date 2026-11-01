ALTER TABLE vendor_employee_addresses
  ADD COLUMN home_employee_slot BINARY(16)
    GENERATED ALWAYS AS (CASE WHEN category = 'HOME' THEN vendor_employee_id ELSE NULL END) STORED,
  ADD COLUMN office_employee_slot BINARY(16)
    GENERATED ALWAYS AS (CASE WHEN category = 'OFFICE' THEN vendor_employee_id ELSE NULL END) STORED,
  ADD UNIQUE KEY uq_vendor_employee_home_address (home_employee_slot),
  ADD UNIQUE KEY uq_vendor_employee_office_address (office_employee_slot);

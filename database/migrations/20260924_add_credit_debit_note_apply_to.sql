-- Make the credit/debit note target explicit and allow either target relation.
ALTER TABLE credit_debit_notes
  ADD COLUMN IF NOT EXISTS apply_to VARCHAR(32) NULL;

UPDATE credit_debit_notes
SET apply_to = CASE
  WHEN organisation_id IS NOT NULL THEN 'ORGANISATION'
  WHEN vendor_partner_id IS NOT NULL THEN 'VENDOR_PARTNER'
  ELSE NULL
END
WHERE apply_to IS NULL;

ALTER TABLE credit_debit_notes
  MODIFY COLUMN apply_to VARCHAR(32) NOT NULL,
  MODIFY COLUMN organisation_id BINARY(16) NULL,
  MODIFY COLUMN vendor_partner_id BINARY(16) NULL;
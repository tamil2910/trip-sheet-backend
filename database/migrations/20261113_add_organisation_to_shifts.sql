ALTER TABLE shifts
    ADD COLUMN organisation_id BINARY(16) NULL AFTER tenant_id;

UPDATE shifts
SET organisation_id = tenant_id
WHERE organisation_id IS NULL;

ALTER TABLE shifts
    MODIFY COLUMN organisation_id BINARY(16) NOT NULL,
    ADD INDEX idx_shift_organisation_deleted (organisation_id, is_deleted),
    ADD CONSTRAINT fk_shift_organisation FOREIGN KEY (organisation_id) REFERENCES tenants (id);

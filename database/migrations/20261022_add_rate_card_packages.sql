CREATE TABLE IF NOT EXISTS rate_card_packages (
    id BINARY(16) NOT NULL,
    created_at BIGINT NULL,
    updated_at BIGINT NULL,
    deleted_at BIGINT NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted_by VARCHAR(255) NULL,
    is_deleted BIT NULL,
    name VARCHAR(255) NOT NULL,
    valid_from BIGINT NULL,
    valid_to BIGINT NULL,
    is_default BIT NOT NULL DEFAULT b'0',
    vendor_partner_id BINARY(16) NULL,
    vendor_organisation_id BINARY(16) NULL,
    PRIMARY KEY (id),
    KEY idx_rate_card_package_partner_default (vendor_partner_id, is_default, is_deleted),
    KEY idx_rate_card_package_organisation_default (vendor_organisation_id, is_default, is_deleted),
    CONSTRAINT fk_rate_card_package_partner FOREIGN KEY (vendor_partner_id) REFERENCES vendor_partners (id),
    CONSTRAINT fk_rate_card_package_organisation FOREIGN KEY (vendor_organisation_id) REFERENCES vendor_organisations (id),
    CONSTRAINT chk_rate_card_package_owner CHECK (
        (vendor_partner_id IS NOT NULL AND vendor_organisation_id IS NULL)
        OR (vendor_partner_id IS NULL AND vendor_organisation_id IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS rate_card_package_items (
    id BINARY(16) NOT NULL,
    created_at BIGINT NULL,
    updated_at BIGINT NULL,
    deleted_at BIGINT NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted_by VARCHAR(255) NULL,
    is_deleted BIT NULL,
    rate_card_package_id BINARY(16) NOT NULL,
    vendor_partner_rate_card_id BINARY(16) NULL,
    vendor_organisation_rate_card_id BINARY(16) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rate_card_package_partner_item (rate_card_package_id, vendor_partner_rate_card_id),
    UNIQUE KEY uq_rate_card_package_organisation_item (rate_card_package_id, vendor_organisation_rate_card_id),
    CONSTRAINT fk_rcpi_package FOREIGN KEY (rate_card_package_id) REFERENCES rate_card_packages (id),
    CONSTRAINT fk_rcpi_partner_rate FOREIGN KEY (vendor_partner_rate_card_id) REFERENCES vendor_partner_rate_cards (id),
    CONSTRAINT fk_rcpi_organisation_rate FOREIGN KEY (vendor_organisation_rate_card_id) REFERENCES vendor_organisation_rate_cards (id),
    CONSTRAINT chk_rcpi_one_rate_card CHECK (
        (vendor_partner_rate_card_id IS NOT NULL AND vendor_organisation_rate_card_id IS NULL)
        OR (vendor_partner_rate_card_id IS NULL AND vendor_organisation_rate_card_id IS NOT NULL)
    )
);

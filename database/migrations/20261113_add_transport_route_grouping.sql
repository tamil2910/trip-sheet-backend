CREATE TABLE IF NOT EXISTS transport_routes (
    id BINARY(16) NOT NULL,
    created_at BIGINT NULL,
    updated_at BIGINT NULL,
    deleted_at BIGINT NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted_by VARCHAR(255) NULL,
    is_deleted BIT NULL,
    organisation_id BINARY(16) NOT NULL,
    shift_id BINARY(16) NOT NULL,
    route_direction VARCHAR(255) NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    pickup_latitude DECIMAL(10,7) NOT NULL,
    pickup_longitude DECIMAL(10,7) NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    dropoff_latitude DECIMAL(10,7) NOT NULL,
    dropoff_longitude DECIMAL(10,7) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_transport_route_org_shift (organisation_id, shift_id, is_deleted),
    CONSTRAINT fk_transport_route_organisation FOREIGN KEY (organisation_id) REFERENCES tenants (id),
    CONSTRAINT fk_transport_route_shift FOREIGN KEY (shift_id) REFERENCES shifts (id)
);

ALTER TABLE transport_requests
    ADD COLUMN route_id BINARY(16) NULL,
    ADD COLUMN pickup_time BIGINT NULL,
    ADD INDEX idx_transport_request_route (route_id),
    ADD CONSTRAINT fk_transport_request_route FOREIGN KEY (route_id) REFERENCES transport_routes (id);

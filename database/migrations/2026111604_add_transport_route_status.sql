ALTER TABLE transport_routes
    ADD COLUMN route_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

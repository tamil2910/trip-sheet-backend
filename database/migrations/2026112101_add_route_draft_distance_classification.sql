ALTER TABLE transport_route_drafts
  ADD COLUMN total_route_km DOUBLE NULL,
  ADD COLUMN route_length_category VARCHAR(16) NULL;

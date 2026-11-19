ALTER TABLE transport_route_drafts
  ADD COLUMN valid_from BIGINT NULL,
  ADD COLUMN valid_to BIGINT NULL;

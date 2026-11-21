-- Controls whether a trip delegated to a partner vendor appears in that vendor's trip list.
ALTER TABLE trips
  ADD COLUMN is_share_to_partner BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN partner_response_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SHARED';

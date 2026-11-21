-- Controls whether a trip delegated to a partner vendor appears in that vendor's trip list.
ALTER TABLE trips
  ADD COLUMN is_share_to_partner BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN partner_response_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SHARED';

ALTER TABLE trip_vendor_delegation_history
  ADD COLUMN delegation_status VARCHAR(30) NULL,
  ADD COLUMN response_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SHARED';

UPDATE trip_vendor_delegation_history delegation
JOIN trips trip ON trip.id = delegation.trip_id
SET delegation.delegation_status = trip.trip_status,
    delegation.response_status = CASE
      WHEN trip.is_share_to_partner = TRUE AND trip.partner_response_status = 'PENDING' THEN 'PENDING'
      WHEN trip.is_share_to_partner = TRUE AND trip.partner_response_status = 'DECLINED' THEN 'DECLINED'
      WHEN trip.is_share_to_partner = TRUE THEN 'ACCEPTED'
      ELSE 'NOT_SHARED'
    END;

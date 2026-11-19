ALTER TABLE transport_events
  ADD COLUMN public_request_link VARCHAR(2048) NULL;

ALTER TABLE transport_event_request_invites
  ADD COLUMN request_link VARCHAR(2048) NULL;

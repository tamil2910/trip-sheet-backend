ALTER TABLE transport_events
  ADD COLUMN allowed_extra_passengers INT NOT NULL DEFAULT 0;

ALTER TABLE transport_event_participant_extra_passengers
  ADD COLUMN relationship_type VARCHAR(64) NOT NULL DEFAULT 'OTHER';

ALTER TABLE transport_events
  ADD COLUMN bring_family BIT(1) NOT NULL DEFAULT b'0';

ALTER TABLE transport_event_participant_requests
  ADD COLUMN gender VARCHAR(32) NULL;

CREATE TABLE IF NOT EXISTS transport_event_participant_extra_passengers (
  id BINARY(16) NOT NULL PRIMARY KEY,
  participant_request_id BINARY(16) NOT NULL,
  name VARCHAR(255) NOT NULL,
  gender VARCHAR(32) NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_event_extra_passenger_request FOREIGN KEY (participant_request_id)
    REFERENCES transport_event_participant_requests(id),
  INDEX idx_event_extra_passenger_request (participant_request_id)
);

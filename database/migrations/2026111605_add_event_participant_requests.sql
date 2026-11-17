ALTER TABLE transport_event_request_invites
  ADD COLUMN consumed_at BIGINT NULL;

CREATE TABLE IF NOT EXISTS transport_event_participant_requests (
  id BINARY(16) NOT NULL PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  invite_id BINARY(16) NULL,
  people_tenant_id BINARY(16) NULL,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(320) NULL,
  phone VARCHAR(32) NULL,
  pickup_address VARCHAR(1000) NULL,
  pickup_latitude DOUBLE NOT NULL,
  pickup_longitude DOUBLE NOT NULL,
  notes VARCHAR(2000) NULL,
  status VARCHAR(32) NOT NULL,
  match_method VARCHAR(20) NOT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_event_participant_request_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  CONSTRAINT fk_event_participant_request_invite FOREIGN KEY (invite_id) REFERENCES transport_event_request_invites(id),
  CONSTRAINT fk_event_participant_request_people FOREIGN KEY (people_tenant_id) REFERENCES people_tenant(id),
  INDEX idx_event_participant_request_event_status (event_id, status, created_at),
  INDEX idx_event_participant_request_people (people_tenant_id)
);

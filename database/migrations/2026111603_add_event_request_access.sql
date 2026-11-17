ALTER TABLE transport_events
  ADD COLUMN public_request_token_hash VARCHAR(64) NULL,
  ADD UNIQUE INDEX uk_transport_events_public_request_token (public_request_token_hash);

CREATE TABLE IF NOT EXISTS transport_event_request_invites (
  id BINARY(16) NOT NULL PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(320) NOT NULL,
  phone VARCHAR(32) NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at BIGINT NOT NULL,
  revoked_at BIGINT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_transport_event_invites_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  UNIQUE INDEX uk_transport_event_invites_token (token_hash),
  INDEX idx_transport_event_invites_event (event_id, revoked_at, is_deleted)
);

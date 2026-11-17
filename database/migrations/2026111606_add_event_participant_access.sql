ALTER TABLE transport_event_participant_requests
  ADD COLUMN trip_id BINARY(16) NULL,
  ADD COLUMN reviewed_at BIGINT NULL,
  ADD COLUMN reviewed_by VARCHAR(255) NULL,
  ADD COLUMN decision_reason VARCHAR(1000) NULL,
  ADD CONSTRAINT fk_event_participant_request_trip FOREIGN KEY (trip_id) REFERENCES trips(id);

CREATE TABLE IF NOT EXISTS event_participant_access_grants (
  id BINARY(16) NOT NULL PRIMARY KEY,
  participant_request_id BINARY(16) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at BIGINT NOT NULL,
  consumed_at BIGINT NULL,
  revoked_at BIGINT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_event_access_grant_request FOREIGN KEY (participant_request_id) REFERENCES transport_event_participant_requests(id),
  UNIQUE INDEX uk_event_access_grant_token (token_hash),
  INDEX idx_event_access_grant_request (participant_request_id, revoked_at)
);

CREATE TABLE IF NOT EXISTS event_participant_otp_challenges (
  id BINARY(16) NOT NULL PRIMARY KEY,
  access_grant_id BINARY(16) NOT NULL,
  otp_hash VARCHAR(100) NOT NULL,
  expires_at BIGINT NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  consumed_at BIGINT NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_event_otp_grant FOREIGN KEY (access_grant_id) REFERENCES event_participant_access_grants(id),
  INDEX idx_event_otp_grant_created (access_grant_id, created_at)
);

CREATE TABLE IF NOT EXISTS event_participant_access_sessions (
  id BINARY(16) NOT NULL PRIMARY KEY,
  access_grant_id BINARY(16) NOT NULL,
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
  CONSTRAINT fk_event_access_session_grant FOREIGN KEY (access_grant_id) REFERENCES event_participant_access_grants(id),
  UNIQUE INDEX uk_event_access_session_token (token_hash),
  INDEX idx_event_access_session_grant (access_grant_id, expires_at, revoked_at)
);

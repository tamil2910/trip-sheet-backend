CREATE TABLE event_vendor_transport_requests (
  id BINARY(16) NOT NULL PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  vendor_id BINARY(16) NOT NULL,
  format VARCHAR(16) NOT NULL,
  transport_type VARCHAR(32) NOT NULL,
  vehicle_type_id BINARY(16) NOT NULL,
  duty_type_id BINARY(16) NOT NULL,
  pickup_time BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  trip_id BINARY(16) NULL,
  created_at BIGINT NULL,
  updated_at BIGINT NULL,
  deleted_at BIGINT NULL,
  created_by VARCHAR(255) NULL,
  updated_by VARCHAR(255) NULL,
  deleted_by VARCHAR(255) NULL,
  is_deleted BIT(1) NULL DEFAULT b'0',
  CONSTRAINT fk_event_vendor_request_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  CONSTRAINT fk_event_vendor_request_vendor FOREIGN KEY (vendor_id) REFERENCES tenants(id),
  CONSTRAINT fk_event_vendor_request_trip FOREIGN KEY (trip_id) REFERENCES trips(id),
  INDEX idx_event_vendor_request_vendor_status (vendor_id, status, created_at)
);

CREATE TABLE event_vendor_transport_request_participants (
  transport_request_id BINARY(16) NOT NULL,
  participant_request_id BINARY(16) NOT NULL,
  PRIMARY KEY (transport_request_id, participant_request_id),
  CONSTRAINT fk_event_vendor_participant_request FOREIGN KEY (transport_request_id) REFERENCES event_vendor_transport_requests(id),
  CONSTRAINT fk_event_vendor_participant_participant FOREIGN KEY (participant_request_id) REFERENCES transport_event_participant_requests(id),
  INDEX idx_event_vendor_participant_id (participant_request_id)
);

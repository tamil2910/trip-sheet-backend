CREATE TABLE IF NOT EXISTS transport_event_participant_transport_types (
  participant_request_id BINARY(16) NOT NULL,
  transport_type VARCHAR(32) NOT NULL,
  transport_type_order INT NOT NULL,
  PRIMARY KEY (participant_request_id, transport_type_order),
  CONSTRAINT fk_event_participant_transport_types_request
    FOREIGN KEY (participant_request_id) REFERENCES transport_event_participant_requests(id),
  INDEX idx_event_participant_transport_types_type (transport_type)
);

CREATE TABLE IF NOT EXISTS transport_event_transport_types (
  event_id BINARY(16) NOT NULL,
  transport_type VARCHAR(32) NOT NULL,
  transport_type_order INT NOT NULL,
  PRIMARY KEY (event_id, transport_type_order),
  CONSTRAINT fk_event_transport_types_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  INDEX idx_event_transport_types_type (transport_type)
);

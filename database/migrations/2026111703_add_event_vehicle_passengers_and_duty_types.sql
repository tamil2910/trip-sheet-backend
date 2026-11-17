ALTER TABLE transport_event_vehicle_types
  ADD COLUMN allowed_passenger INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS transport_event_duty_types (
  event_id BINARY(16) NOT NULL,
  duty_type_id BINARY(16) NOT NULL,
  PRIMARY KEY (event_id, duty_type_id),
  CONSTRAINT fk_event_duty_types_event FOREIGN KEY (event_id) REFERENCES transport_events(id),
  CONSTRAINT fk_event_duty_types_duty_type FOREIGN KEY (duty_type_id) REFERENCES duty_types(id),
  INDEX idx_event_duty_types_duty_type (duty_type_id)
);

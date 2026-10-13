CREATE TABLE IF NOT EXISTS trip_location_updates (
  id BINARY(16) NOT NULL,
  trip_id BINARY(16) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  recorded_at BIGINT NOT NULL,
  accuracy_meters DOUBLE NULL,
  speed_meters_per_second DOUBLE NULL,
  received_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_trip_location_trip_recorded (trip_id, recorded_at),
  CONSTRAINT fk_trip_location_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
);

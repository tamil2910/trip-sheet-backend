CREATE TABLE IF NOT EXISTS trip_stop_passengers (
  trip_stop_id BINARY(16) NOT NULL,
  passenger_id BINARY(16) NOT NULL,
  pickup_time BIGINT NOT NULL,
  PRIMARY KEY (trip_stop_id, passenger_id),
  CONSTRAINT fk_trip_stop_passengers_stop
    FOREIGN KEY (trip_stop_id) REFERENCES trip_stops(id) ON DELETE CASCADE
) ENGINE=InnoDB;

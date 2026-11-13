ALTER TABLE trip_location_updates
  ADD COLUMN event_type VARCHAR(40) NULL,
  ADD COLUMN previous_speed_meters_per_second DOUBLE NULL,
  ADD COLUMN acceleration_meters_per_second_squared DOUBLE NULL,
  ADD COLUMN event_source VARCHAR(20) NULL;

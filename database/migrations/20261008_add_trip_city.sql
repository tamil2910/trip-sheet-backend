-- Saves the rate-card city selected for each trip.
ALTER TABLE trips
  ADD COLUMN city VARCHAR(255) NULL;

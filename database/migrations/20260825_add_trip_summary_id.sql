ALTER TABLE trips
  ADD COLUMN IF NOT EXISTS trip_summary_id BINARY(16) NULL;

UPDATE trips trip
JOIN trip_summaries summary ON summary.trip_id = trip.id
SET trip.trip_summary_id = summary.id
WHERE trip.trip_summary_id IS NULL;

SET @trip_airport_transfer_type_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'trips'
    AND column_name = 'airport_transfer_type'
);
SET @trip_airport_transfer_type_sql := IF(
  @trip_airport_transfer_type_exists = 0,
  'ALTER TABLE trips ADD COLUMN airport_transfer_type VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE trip_airport_transfer_type_statement FROM @trip_airport_transfer_type_sql;
EXECUTE trip_airport_transfer_type_statement;
DEALLOCATE PREPARE trip_airport_transfer_type_statement;
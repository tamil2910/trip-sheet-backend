CREATE TABLE IF NOT EXISTS trip_labels (
  trip_id BINARY(16) NOT NULL,
  label_id BINARY(16) NOT NULL,
  PRIMARY KEY (trip_id, label_id),
  INDEX idx_trip_labels_label_id (label_id),
  CONSTRAINT fk_trip_labels_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
  CONSTRAINT fk_trip_labels_label FOREIGN KEY (label_id) REFERENCES labels (id)
);

CREATE INDEX IF NOT EXISTS idx_trip_labels_label_id ON trip_labels (label_id);

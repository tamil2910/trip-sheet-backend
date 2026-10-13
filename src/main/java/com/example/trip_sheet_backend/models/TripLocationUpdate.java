package com.example.trip_sheet_backend.models;

import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_location_updates", indexes = {
    @Index(name = "idx_trip_location_trip_recorded", columnList = "trip_id, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
public class TripLocationUpdate {
  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trip_id", nullable = false)
  private Trip trip;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  @Column(name = "recorded_at", nullable = false)
  private Long recordedAt;

  private Double accuracyMeters;
  private Double speedMetersPerSecond;

  @Column(name = "received_at", nullable = false)
  private Long receivedAt;
}

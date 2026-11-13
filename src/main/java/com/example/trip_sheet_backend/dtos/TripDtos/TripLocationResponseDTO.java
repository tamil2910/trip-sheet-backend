package com.example.trip_sheet_backend.dtos.TripDtos;

import com.example.trip_sheet_backend.models.TripLocationUpdate;
import com.example.trip_sheet_backend.models.TripLocationUpdate.DrivingEventSource;
import com.example.trip_sheet_backend.models.TripLocationUpdate.DrivingEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripLocationResponseDTO {
  private String tripId;
  private Double latitude;
  private Double longitude;
  private Long recordedAt;
  private Double accuracyMeters;
  private Double speedMetersPerSecond;
  private DrivingEventType eventType;
  private Double previousSpeedMetersPerSecond;
  private Double accelerationMetersPerSecondSquared;
  private DrivingEventSource eventSource;
  private Long receivedAt;

  public static TripLocationResponseDTO from(TripLocationUpdate update) {
    return new TripLocationResponseDTO(update.getTrip().getId().toString(), update.getLatitude(),
        update.getLongitude(), update.getRecordedAt(), update.getAccuracyMeters(),
        update.getSpeedMetersPerSecond(), update.getEventType(), update.getPreviousSpeedMetersPerSecond(),
        update.getAccelerationMetersPerSecondSquared(), update.getEventSource(), update.getReceivedAt());
  }
}

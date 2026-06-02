package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TripRealtimeEventDTO {

  private TripRealtimeEventType eventType;
  private String tripId;
  private TripResponseDTO trip;
  private Long emittedAt;

  public enum TripRealtimeEventType {
    CREATED,
    UPDATED,
    DELETED
  }
}

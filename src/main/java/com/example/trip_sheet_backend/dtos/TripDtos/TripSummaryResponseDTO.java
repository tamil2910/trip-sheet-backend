package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripSummaryResponseDTO {
  private String id;
  private String tripId;
  private TripResponseDTO trip;
  private Long garageStartTime;
  private Long garageEndTime;
  private Long tripArrivedTime;
  private Long tripStartTime;
  private Long tripStartKmOdo;
  private Long tripStartKmOdoImage;
  private Long tripEndTime;
  private Long tripEndKmOdo;
  private Long tripEndKmOdoImage;
  private Long tripDuration;
  private Long tripDistance;
  private Long tripExtraKmOdo;
  private Long tripExtraKm;
  private Long tripExtraHr;
  private Long tripStartGPSKM;
  private Long tripEndGPSKM;
  private Long tripGPSDuration;
  private Long tripGPSDistance;
  private Double dispatchLat;
  private Double dispatchLng;
  private Double arrivedLat;
  private Double arrivedLng;
  private Double tripStartLat;
  private Double tripStartLng;
  private Double tripEndLat;
  private Double tripEndLng;
  private Double garageEndLat;
  private Double garageEndLng;
}

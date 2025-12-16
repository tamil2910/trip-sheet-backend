package com.example.trip_sheet_backend.dtos;

import com.example.trip_sheet_backend.models.TripStop;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripStopRequestDTO {

  private Integer sequenceNumber;

  private TripStop.StopType stopType;

  private String addressText;
  private String formattedAddress;

  private Double latitude;
  private Double longitude;
}

package com.example.trip_sheet_backend.dtos.TripDtos;

import com.example.trip_sheet_backend.models.TripStop;
import java.util.List;
import jakarta.validation.Valid;

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

  @Valid
  private List<TripStopPassengerRequestDTO> passengers;
}

package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripAllotRequestDTO {
  private String driverId;
  private String vehicleId;
  private String dispatchCenterId;
}

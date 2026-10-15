package com.example.trip_sheet_backend.dtos.TripDtos;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualTripExecuteRequestDTO {
  private TripDispatchRequestDTO dispatchData;
  private TripArrivedRequestDTO arrivedData;
  private TripStartRequestDTO startData;
  private TripDropRequestDTO dropData;
  private String tripRouteUrl;
  private BigDecimal tripRouteGpsKm;
}

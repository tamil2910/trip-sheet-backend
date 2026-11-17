package com.example.trip_sheet_backend.dtos.EventDtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventRouteDraftRequest {
  @DecimalMin("0.1")
  @DecimalMax("4.0")
  private Double maxPickupDistanceKm = 4.0;
}

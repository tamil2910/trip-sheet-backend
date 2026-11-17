package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEventVehicleTypesRequest {
  @NotNull
  private List<UUID> vehicleTypeIds;
}


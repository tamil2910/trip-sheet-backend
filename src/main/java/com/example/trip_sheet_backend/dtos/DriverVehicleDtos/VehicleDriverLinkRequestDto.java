package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDriverLinkRequestDto {

  @NotNull(message = "Driver id is required")
  private UUID driverId;

  @NotNull(message = "Vehicle id is required")
  private UUID vehicleId;
}

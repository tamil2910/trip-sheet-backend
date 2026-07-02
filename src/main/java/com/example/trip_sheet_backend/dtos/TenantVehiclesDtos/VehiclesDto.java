package com.example.trip_sheet_backend.dtos.TenantVehiclesDtos;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class VehiclesDto {
  private UUID id;
  private String vehicleNumber;
  private String vehicleUniqueCode;
  private VehicleTypeDto vehicleType;
}

@Getter
@Setter
class VehicleTypeDto {
  private UUID id;
  private String typeOfVehicle;
  private Integer seatCount;
}
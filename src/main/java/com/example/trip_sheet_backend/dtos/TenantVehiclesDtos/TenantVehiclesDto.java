package com.example.trip_sheet_backend.dtos.TenantVehiclesDtos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantVehiclesDto {
  private UUID tenantId;
  private String tenantName;
  private List<VehiclesDto> vehicles;
}

package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateOrLinkResponseDto {
  private String action;
  private String uniqueCode;
  private Boolean vehicleExists;
  private Boolean linkedToTenant;
  private UUID tenantId;
  private UUID mappingId;
  private Vehicle vehicle;

  public static VehicleCreateOrLinkResponseDto fromEntity(
      String action,
      Vehicle vehicle,
      VehicleTenantMapping mapping,
      boolean vehicleExists
  ) {
    return new VehicleCreateOrLinkResponseDto(
        action,
        vehicle != null ? vehicle.getVehicleUniqueCode() : null,
        vehicleExists,
        mapping != null,
        mapping != null && mapping.getTenant() != null ? mapping.getTenant().getId() : null,
        mapping != null ? mapping.getId() : null,
        vehicle
    );
  }
}

package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Tenant;
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
public class VehicleCodeLookupResponseDto {
  private String uniqueCode;
  private Boolean linkedToCurrentTenant;
  private UUID tenantId;
  private UUID mappingId;
  private Boolean activeForTenant;
  private VehicleTenantResponseDto.VehicleSummary vehicle;

  public static VehicleCodeLookupResponseDto fromEntity(
      Vehicle vehicleEntity,
      Tenant currentTenant,
      VehicleTenantMapping mapping
  ) {
    VehicleTenantResponseDto.VehicleSummary vehicle = new VehicleTenantResponseDto.VehicleSummary(
        vehicleEntity != null ? vehicleEntity.getId() : null,
        vehicleEntity != null ? vehicleEntity.getVehicleUniqueCode() : null,
        vehicleEntity != null ? vehicleEntity.getModelName() : null,
        vehicleEntity != null ? vehicleEntity.getVehicleNumber() : null,
        vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getId() : null,
        vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getDefaultName() : null,
        vehicleEntity != null ? vehicleEntity.getDescription() : null,
        vehicleEntity != null ? vehicleEntity.getColour() : null,
        vehicleEntity != null ? vehicleEntity.getFuelType() : null,
        mapping != null ? mapping.getActive() : vehicleEntity != null ? vehicleEntity.getIsActive() : null,
        vehicleEntity != null ? vehicleEntity.getRegisteredOwnerName() : null
    );

    return new VehicleCodeLookupResponseDto(
        vehicleEntity != null ? vehicleEntity.getVehicleUniqueCode() : null,
        mapping != null,
        currentTenant != null ? currentTenant.getId() : null,
        mapping != null ? mapping.getId() : null,
        mapping != null ? mapping.getActive() : null,
        vehicle
    );
  }
}

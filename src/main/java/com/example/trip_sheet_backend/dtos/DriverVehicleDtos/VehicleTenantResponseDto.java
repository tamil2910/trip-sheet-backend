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
public class VehicleTenantResponseDto {
  private UUID mappingId;
  private UUID tenantId;
  private Boolean activeForTenant;
  private Long linkedAt;
  private VehicleSummary vehicle;

  public static VehicleTenantResponseDto fromEntity(VehicleTenantMapping mapping) {
    Vehicle vehicleEntity = mapping.getVehicle();

    VehicleSummary vehicle = new VehicleSummary(
        vehicleEntity != null ? vehicleEntity.getId() : null,
        vehicleEntity != null ? vehicleEntity.getVehicleUniqueCode() : null,
        vehicleEntity != null ? vehicleEntity.getModelName() : null,
        vehicleEntity != null ? vehicleEntity.getVehicleNumber() : null,
        vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getId() : null,
        vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getDefaultName() : null,
        vehicleEntity != null ? vehicleEntity.getDescription() : null,
        vehicleEntity != null ? vehicleEntity.getColour() : null,
        vehicleEntity != null ? vehicleEntity.getFuelType() : null,
        mapping.getActive(),
        vehicleEntity != null ? vehicleEntity.getRegisteredOwnerName() : null
    );

    return new VehicleTenantResponseDto(
        mapping.getId(),
        mapping.getTenant() != null ? mapping.getTenant().getId() : null,
        mapping.getActive(),
        mapping.getLinkedAt(),
        vehicle
    );
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VehicleSummary {
    private UUID id;
    private String vehicleUniqueCode;
    private String modelName;
    private String vehicleNumber;
    private UUID vehicleTypeId;
    private String vehicleTypeName;
    private String description;
    private String colour;
    private Vehicle.typeFuel fuelType;
    private Boolean active;
    private String registeredOwnerName;
  }
}

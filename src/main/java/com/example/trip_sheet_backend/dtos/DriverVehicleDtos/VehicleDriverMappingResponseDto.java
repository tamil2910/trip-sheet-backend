package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDriverMappingResponseDto {
  private UUID mappingId;
  private Boolean isActive;
  private DriverSummary driver;
  private VehicleSummary vehicle;

  public static VehicleDriverMappingResponseDto fromEntity(VehicleDriverMapping mapping) {
    Driver driverEntity = mapping.getDriver();
    Vehicle vehicleEntity = mapping.getVehicle();
    UserAccount account = driverEntity != null ? driverEntity.getAccount() : null;

    DriverSummary driver = new DriverSummary(
      driverEntity != null ? driverEntity.getId() : null,
      driverEntity != null ? driverEntity.getFullName() : null,
      driverEntity != null ? driverEntity.getLicenseNumber() : null,
      driverEntity != null ? driverEntity.getAvailable() : null,
      driverEntity != null ? driverEntity.getActive() : null,
      account != null ? account.getEmail() : null,
      account != null ? account.getPhone() : null
    );

    VehicleSummary vehicle = new VehicleSummary(
      vehicleEntity != null ? vehicleEntity.getId() : null,
      vehicleEntity != null ? vehicleEntity.getVehicleNumber() : null,
      vehicleEntity != null ? vehicleEntity.getModelName() : null,
      vehicleEntity != null ? vehicleEntity.getDescription() : null,
      vehicleEntity != null ? vehicleEntity.getFuelType() : null,
      vehicleEntity != null ? vehicleEntity.getIsActive() : null,
      vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getId() : null,
      vehicleEntity != null && vehicleEntity.getVehicleType() != null ? vehicleEntity.getVehicleType().getDefaultName() : null
    );

    return new VehicleDriverMappingResponseDto(
      mapping.getId(),
      mapping.getIsActive(),
      driver,
      vehicle
    );
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DriverSummary {
    private UUID id;
    private String fullName;
    private String licenseNumber;
    private Boolean available;
    private Boolean active;
    private String email;
    private String phone;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VehicleSummary {
    private UUID id;
    private String vehicleNumber;
    private String modelName;
    private String description;
    private Vehicle.typeFuel fuelType;
    private Boolean isActive;
    private UUID vehicleTypeId;
    private String vehicleTypeName;
  }
}

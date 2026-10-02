package com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDriverVehiclesResponseDTO {
  private UUID id;
  private UUID primaryVendorId;
  private UUID partnerVendorId;
  private Type type;
  private String driverName;
  private String phone;
  private String email;
  private UUID driverId;
  private String vehicleNumber;
  private UUID vehicleTypeId;

  public static SupplierDriverVehiclesResponseDTO fromEntity(SupplierDriverVehicles entity) {
    return new SupplierDriverVehiclesResponseDTO(
        entity.getId(),
        entity.getPrimaryVendor().getId(),
        entity.getPartnerVendor().getId(),
        entity.getType(),
        entity.getDriverName(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getDriver() == null ? null : entity.getDriver().getId(),
        entity.getVehicleNumber(),
        entity.getVehicleType() == null ? null : entity.getVehicleType().getId()
    );
  }
}
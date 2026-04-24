package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import com.example.trip_sheet_backend.models.Vehicle;

import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequestDto {

  @Nullable
  private String modelName;

  @Nullable
  private String vehicleNumber;

  @Nullable
  private String vehicleTypeId;

  @Nullable
  private Vehicle.typeFuel fuelType;

  @Nullable
  private String colour;

  @Nullable
  private String description;

  @Nullable
  private String leftSideUrl;

  @Nullable
  private String rightSideUrl;

  @Nullable
  private String backSideUrl;

  @Nullable
  private String frontSideUrl;

  @Nullable
  private String vehProfileUrl;

  @Nullable
  private String registeredOwnerName;

  @Nullable
  private String registrationDate;

  @Nullable
  private String chassisNumber;

  @Nullable
  private String engineNumber;

  @Nullable
  private String insuranceCompanyName;

  @Nullable
  private String policyNumber;

  @Nullable
  private String issueDate;

  @Nullable
  private String dueDate;

  @Nullable
  private String premiumAmount;

  @Nullable
  private String coverAmount;
}

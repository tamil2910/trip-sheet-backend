package com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.SupplierDriverVehicles.Type;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupplierDriverVehiclesCreateRequestDTO {

  @NotNull
  private UUID partnerVendorId;

  @NotNull
  private Type type;

  private String driverName;

  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a 10-digit Indian mobile number starting with 6-9")
  private String phone;

  @Email(message = "Invalid email format")
  private String email;
  private String vehicleNumber;
  private UUID vehicleTypeId;
}
package com.example.trip_sheet_backend.dtos.DriverDtos;

import com.example.trip_sheet_backend.models.Driver;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverCreateOrLinkRequestDto {

  @Nullable
  private String uniqueCode;

  @Nullable
  private String username;

  @Nullable
  private String fullName;

  @Nullable
  @Email(message = "Invalid email format")
  private String email;

  @Nullable
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
  private String phone;

  @Nullable
  private String password;

  @Nullable
  private String profilePicture;

  @Nullable
  private String licenseNumber;

  @Nullable
  private Long licenseExpiry;

  @Nullable
  private String insuranceNumber;

  @Nullable
  private Long insuranceExpiry;

  @Nullable
  private String policeVerificationId;

  @Nullable
  private String bloodGroup;

  @Nullable
  private Driver.DriverType driverType = Driver.DriverType.PERMANENT;

  @Nullable
  private Double rating = 0.0;

  @Nullable
  private Boolean active = true;

  @Nullable
  private Boolean available = true;
}

package com.example.trip_sheet_backend.dtos.DriverDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.DriverTenantMapping;
import com.example.trip_sheet_backend.models.UserAccount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverTenantResponseDto {
  private UUID mappingId;
  private UUID tenantId;
  private Boolean activeForTenant;
  private Long linkedAt;
  private DriverSummary driver;

  public static DriverTenantResponseDto fromEntity(DriverTenantMapping mapping) {
    Driver driverEntity = mapping.getDriver();
    UserAccount account = driverEntity != null ? driverEntity.getAccount() : null;

    DriverSummary driver = new DriverSummary(
        driverEntity != null ? driverEntity.getId() : null,
        driverEntity != null ? driverEntity.getDriverCode() : null,
        driverEntity != null ? driverEntity.getFullName() : null,
        driverEntity != null ? driverEntity.getProfilePicture() : null,
        driverEntity != null ? driverEntity.getLicenseNumber() : null,
        driverEntity != null ? driverEntity.getLicenseExpiry() : null,
        driverEntity != null ? driverEntity.getInsuranceNumber() : null,
        driverEntity != null ? driverEntity.getInsuranceExpiry() : null,
        driverEntity != null ? driverEntity.getPoliceVerificationId() : null,
        driverEntity != null ? driverEntity.getBloodGroup() : null,
        driverEntity != null ? driverEntity.getRating() : null,
        mapping.getActive(),
        driverEntity != null ? driverEntity.getAvailable() : null,
        driverEntity != null ? driverEntity.getDriverType() : null,
        account != null ? account.getId() : null,
        account != null ? account.getUsername() : null,
        account != null ? account.getEmail() : null,
        account != null ? account.getPhone() : null
    );

    return new DriverTenantResponseDto(
        mapping.getId(),
        mapping.getTenant() != null ? mapping.getTenant().getId() : null,
        mapping.getActive(),
        mapping.getLinkedAt(),
        driver
    );
  }

  public static DriverTenantResponseDto fromDriver(Driver driverEntity) {
    UserAccount account = driverEntity != null ? driverEntity.getAccount() : null;

    DriverSummary driver = new DriverSummary(
        driverEntity != null ? driverEntity.getId() : null,
        driverEntity != null ? driverEntity.getDriverCode() : null,
        driverEntity != null ? driverEntity.getFullName() : null,
        driverEntity != null ? driverEntity.getProfilePicture() : null,
        driverEntity != null ? driverEntity.getLicenseNumber() : null,
        driverEntity != null ? driverEntity.getLicenseExpiry() : null,
        driverEntity != null ? driverEntity.getInsuranceNumber() : null,
        driverEntity != null ? driverEntity.getInsuranceExpiry() : null,
        driverEntity != null ? driverEntity.getPoliceVerificationId() : null,
        driverEntity != null ? driverEntity.getBloodGroup() : null,
        driverEntity != null ? driverEntity.getRating() : null,
        driverEntity != null ? driverEntity.getActive() : null,
        driverEntity != null ? driverEntity.getAvailable() : null,
        driverEntity != null ? driverEntity.getDriverType() : null,
        account != null ? account.getId() : null,
        account != null ? account.getUsername() : null,
        account != null ? account.getEmail() : null,
        account != null ? account.getPhone() : null
    );

    return new DriverTenantResponseDto(
        null,
        null,
        driverEntity != null ? driverEntity.getActive() : null,
        null,
        driver
    );
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DriverSummary {
    private UUID id;
    private String driverCode;
    private String fullName;
    private String profilePicture;
    private String licenseNumber;
    private Long licenseExpiry;
    private String insuranceNumber;
    private Long insuranceExpiry;
    private String policeVerificationId;
    private String bloodGroup;
    private Double rating;
    private Boolean active;
    private Boolean available;
    private Driver.DriverType driverType;
    private UUID accountId;
    private String username;
    private String email;
    private String phone;
  }
}

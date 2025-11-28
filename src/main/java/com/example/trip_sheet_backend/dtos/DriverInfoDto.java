package com.example.trip_sheet_backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DriverInfoDto {

    @NotBlank(message = "Driver full name is required!")
    private String fullName;

    @NotBlank(message = "License number is required!")
    private String licenseNumber;

    @NotNull(message = "License expiry is required!")
    private Long licenseExpiry;

    private String profilePicture;
    private String insuranceNumber;
    private Long insuranceExpiry;
    private String policeVerificationId;
    private String bloodGroup;

    @NotNull(message = "Driver type is required!")
    private DriverType driverType;

    public enum DriverType {
        PERMANENT, CONTRACT, TEMPORARY
    }

    @NotNull(message = "User account ID is required!")
    private String userAccountId;

    @NotNull(message = "Role ID is required!")
    private String roleId;

    private Double rating = 0.0;
    private Boolean active = true;
    private Boolean available = true;
}

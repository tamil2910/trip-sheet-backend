package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DriverInfoDto {

    @Nullable
    private String uniqueCode;

    @Nullable
    private String username;

    @NotBlank(message = "Driver full name is required!")
    private String fullName;

    @Email
    @NotBlank(message = "Driver email is required!")
    private String email;

    @Column(unique = true)
    @Nullable
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be 10 digits and start with 6-9")
    private String phone;

    @Nullable
    @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
      message = "Password must be at least 8 characters and include letters and numbers"
    )
    private String password;

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

    // @NotNull(message = "User account ID is required!")
    // private String userAccountId;

    // @NotNull(message = "Role ID is required!")
    private String roleId;

    private Double rating = 0.0;
    private Boolean active = true;
    private Boolean available = true;
}

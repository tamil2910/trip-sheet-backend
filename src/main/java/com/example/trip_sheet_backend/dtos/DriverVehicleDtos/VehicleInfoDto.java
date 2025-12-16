package com.example.trip_sheet_backend.dtos.DriverVehicleDtos;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleInfoDto {

    @NotBlank(message = "Model name is required!")
    @Size(min = 2, max = 20)
    private String modelName;

    @NotBlank(message = "Vehicle Number is required!")
    private String vehicleNumber;

    @NotNull(message = "Vehicle Type ID is required!")
    private String vehicleTypeId;

    @NotNull(message = "Fuel type is required!")
    private TypeFuel fuelType;

    public enum TypeFuel {
        DIESEL, ELECTRIC, PETROL, CNG, PETROL_CNG
    }

    private String colour;
    private String description;

    @Nullable
    private String vehicleUniqueCode;

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

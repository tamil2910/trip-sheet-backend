package com.example.trip_sheet_backend.dtos;

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

    private String vehicleUniqueCode;

    private String leftSideUrl;
    private String rightSideUrl;
    private String backSideUrl;
    private String frontSideUrl;
    private String vehProfileUrl;

    private String registeredOwnerName;
    private String registrationDate;
    private String chassisNumber;
    private String engineNumber;

    private String insuranceCompanyName;
    private String policyNumber;
    private String issueDate;
    private String dueDate;
    private String premiumAmount;
    private String coverAmount;
}

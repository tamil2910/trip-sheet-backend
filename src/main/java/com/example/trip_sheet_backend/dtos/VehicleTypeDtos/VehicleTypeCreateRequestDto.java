package com.example.trip_sheet_backend.dtos.VehicleTypeDtos;

import com.example.trip_sheet_backend.models.VehicleType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleTypeCreateRequestDto {

    @NotNull(message = "Vehicle type is required")
    private VehicleType.typeVehicle typeOfVehicle; // SEDAN, HATCHBACK, SUV, MUV

    @NotNull(message = "Seat count is required")
    @Min(value = 1, message = "Seat count must be at least 1")
    private Integer seatCount;
    private String description;

    @NotBlank(message = "Custom name cannot be empty")
    private String customName;
    private String isGlobal;
}

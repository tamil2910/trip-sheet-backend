package com.example.trip_sheet_backend.dtos;

import com.example.trip_sheet_backend.models.VehicleType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleTypeCreateRequestDto {
    private VehicleType.typeVehicle typeOfVehicle;
    private Integer seatCount;
    private String description;
    private String customName;
    private String isGlobal;
}

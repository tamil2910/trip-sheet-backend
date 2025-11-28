package com.example.trip_sheet_backend.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDriverCreateRequestDto {
  
    @NotNull(message = "Tenant ID is required!")
    private String tenantId;

    @Valid
    @NotNull(message = "Vehicle info is required!")
    private VehicleInfoDto vehicle_info;

    @Valid
    @NotNull(message = "Driver info is required!")
    private DriverInfoDto driver_info;


    @NotNull(message = "Status is required!")
    private typeStatus status = typeStatus.INACTIVE;

    public enum typeStatus {
        ACTIVE, INACTIVE
    }

}

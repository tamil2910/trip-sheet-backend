package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripSupplierAllotRequestDTO {
    @NotNull(message = "supplierId is required")
    private UUID supplierId;

    @NotNull(message = "vehicleId is required")
    private UUID vehicleId;

    @NotNull(message = "driverId is required")
    private UUID driverId;

    @NotNull(message = "dispatchId is required")
    private UUID dispatchId;
}

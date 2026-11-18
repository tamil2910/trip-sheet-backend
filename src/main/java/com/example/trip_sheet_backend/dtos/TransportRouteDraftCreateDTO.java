package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRouteDraftCreateDTO {
    @NotEmpty(message = "At least one request is required")
    private List<UUID> requestIds;

    /** Deprecated and ignored; route clustering uses pickup/drop-off coordinates and inferred bearing. */
    private TransportRoute.RouteDirection routeDirection;

    /** Defaults to three passengers when omitted. */
    private Integer seatCapacity;

    /** Vehicle types and their usable passenger capacities for choosing each proposed group vehicle. */
    @Valid
    private List<VehicleTypeCapacity> vehicleTypeIds;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VehicleTypeCapacity {
        @NotNull(message = "Vehicle type id is required")
        private UUID id;

        @NotNull(message = "allowedPassenger is required")
        @Min(value = 1, message = "allowedPassenger must be greater than zero")
        private Integer allowedPassenger;
    }
}

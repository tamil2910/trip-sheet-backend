package com.example.trip_sheet_backend.dtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRoute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRouteDraftUpdateDTO {
    @NotNull
    private TransportRoute.RouteDirection routeDirection;
    private UUID vehicleTypeId;
    @NotNull
    private Integer allowedPassenger;
    private String direction;
    private String pickupAddress;
    private String dropoffAddress;
    /** Epoch seconds. */
    @NotNull
    private Long validFrom;
    /** Epoch seconds. */
    @NotNull
    private Long validTo;
    /** Full replacement list: omit a passenger to remove them, include a request to add them. */
    @Valid
    @NotEmpty
    private List<TransportRequestShiftItemDTO> requests;
}

package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** ETS trip payload delegates trip fields to the standard trip create payload. */
@Getter
@Setter
public class EtsTripCreateRequestDTO extends TripCreateRequestDTO {
    @NotNull(message = "allowedPassenger is required from the route analysis group")
    @Min(value = 1, message = "allowedPassenger must be greater than zero")
    private Integer allowedPassenger;
}

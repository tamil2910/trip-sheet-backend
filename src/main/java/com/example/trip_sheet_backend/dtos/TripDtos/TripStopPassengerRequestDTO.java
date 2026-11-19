package com.example.trip_sheet_backend.dtos.TripDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripStopPassengerRequestDTO {
    @NotBlank
    private String passengerId;

    @NotNull
    private Long pickupTime;
}

package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripDispatchResponseDTO {

    private TripResponseDTO trip;
    private String trackingToken;
}
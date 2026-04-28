package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripArrivedRequestDTO {

    private Double arrivedLat;
    private Double arrivedLng;
}

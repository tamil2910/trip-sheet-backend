package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripStartRequestDTO {

    private Long tripStartKmOdo;
    private Long startOtp;
    private Double tripStartLat;
    private Double tripStartLng;
    private Long tripStartTime;
}

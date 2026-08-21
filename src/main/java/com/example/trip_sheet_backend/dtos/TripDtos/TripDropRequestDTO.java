package com.example.trip_sheet_backend.dtos.TripDtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripDropRequestDTO {

    private Long tripEndKmOdo;
    private Long endOtp;
    private Double tripEndLat;
    private Double tripEndLng;
    private Long tripEndTime;
    private Long garageEndTime;
}

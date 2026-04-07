package com.example.trip_sheet_backend.dtos.TripDtos;

import com.example.trip_sheet_backend.models.TripStop;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripStopResponseDTO {

    private String id;

    private Integer sequenceNumber;

    private TripStop.StopType stopType;

    private String addressText;
    private String formattedAddress;

    private Double latitude;
    private Double longitude;
    private Boolean accurate;
}

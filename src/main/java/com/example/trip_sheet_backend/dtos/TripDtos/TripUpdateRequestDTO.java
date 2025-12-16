package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripUpdateRequestDTO {

    private String vendorId;  
    private String driverId;
    private String vehicleId;
    private String dutyTypeId;

    private String notes;

    private List<TripStopRequestDTO> stops; 
}

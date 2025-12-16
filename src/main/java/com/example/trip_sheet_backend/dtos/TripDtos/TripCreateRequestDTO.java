package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripCreateRequestDTO {

    private String bookingId; // optional for single trips

    private String tenantId;  // corporate
    private String vendorId;  // executing vendor

    private String driverId;
    private String vehicleId;
    private String dutyTypeId;

    private String notes;

    private List<TripStopRequestDTO> stops;
}

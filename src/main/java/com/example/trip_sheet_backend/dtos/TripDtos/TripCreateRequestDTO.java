package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripCreateRequestDTO {

    private String tenantId;  // corporate
    private String vendorId;  // executing vendor

    // Corporate owning the trip
    private String organisationId;

    private String driverId;
    private String vehicleId;

    private String dutyTypeId;
    private String vehicleTypeId;

    private String bookerId;
    private String savedPassengerId;

    // epoch seconds
    private Long pickupTime;

    // epoch seconds (conditional)
    private Long endDate;

    private String notes;

    private List<TripStopRequestDTO> stops;
}

package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import com.example.trip_sheet_backend.models.Trip;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripUpdateRequestDTO {

    private String tripCode;
    private Trip.TripType tripType;
    private String parentTripId;

    private Integer recurrenceInterval;
    private String daysOfWeek;
    private Trip.RecurrenceFrequency recurrenceFrequency;

    private String organisationId;
    private String vendorId;  
    private String driverId;
    private String vehicleId;
    private String dutyTypeId;
    private String vehicleTypeId;

    private List<String> passengerIds;
    private String bookerId;
    private List<TripPassengerCustomFieldValueRequestDTO> passengerCustomFieldValues;

    private Long pickupTime;
    private Long startDate;
    private Long endDate;

    private String notes;
    private Boolean isManualTrip;

    private List<TripStopRequestDTO> stops; 
}

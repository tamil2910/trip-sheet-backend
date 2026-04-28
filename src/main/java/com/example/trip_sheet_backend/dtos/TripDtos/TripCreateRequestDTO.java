package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import com.example.trip_sheet_backend.models.Trip;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripCreateRequestDTO {

    private String tripCode;
    private Trip.TripType tripType;
    private String parentTripId;

    // Recurrence fields (used when tripType is RECURRING)
    private Integer recurrenceInterval;
    private String daysOfWeek;
    private Trip.RecurrenceFrequency recurrenceFrequency;

    private String vendorId;  // executing vendor

    // Corporate owning the trip
    private String organisationId;

    private String driverId;
    private String vehicleId;

    private String dutyTypeId;
    private String vehicleTypeId;

    // private String bookerId;
    // private String savedPassengerId;
    private List<String> passengerIds;
    private String bookerId;
    private List<TripPassengerCustomFieldValueRequestDTO> passengerCustomFieldValues;


    // epoch seconds
    private Long pickupTime;

    // epoch seconds (conditional)
    private Long startDate;
    // epoch seconds (conditional)
    private Long endDate;

    private String notes;

    private Boolean isManualTrip;

    private List<TripStopRequestDTO> stops;
}

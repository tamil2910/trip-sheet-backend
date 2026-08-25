package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import com.example.trip_sheet_backend.models.DutyType.TypeAirportTransfer;
import com.example.trip_sheet_backend.models.Trip;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TripResponseDTO {

    private String id;
    private String parentTripId;
    private String tripSummaryId;
    private String tripCode;

    private Trip.TripStatus tripStatus;
    private Trip.TripType tripType;
    private Integer recurrenceInterval;
    private String daysOfWeek;
    private Trip.RecurrenceFrequency recurrenceFrequency;
    private TypeAirportTransfer airportTransferType;

    private TripRelationResponseDTO vendor;
    private TripRelationResponseDTO organisation;
    private TripRelationResponseDTO assignedByVendor;
    private TripRelationResponseDTO previousVendor;

    private TripRelationResponseDTO driver;
    private TripBasicRelationResponseDTO vehicle;
    private TripBasicRelationResponseDTO dispatchCenter;
    private TripBasicRelationResponseDTO vehicleType;
    private TripBasicRelationResponseDTO dutyType;
    private TripRelationResponseDTO booker;
    private List<TripRelationResponseDTO> passengers;
    private List<TripPassengerCustomFieldValueResponseDTO> passengerCustomFieldValues;

    private String notes;

    private Long pickupTime;
    private Long startDate;
    private Long endDate;
    private Long startOtp;
    private Long endOtp;
    private Boolean isManualTrip;

    private List<TripStopResponseDTO> stops;
}

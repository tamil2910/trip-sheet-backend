package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRequest;
import com.example.trip_sheet_backend.models.Trip;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportRequestShiftItemDTO {
    private UUID id;
    private UUID passengerId;
    private TransportRequest.PassengerType passengerType;
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;
    private Trip.TripType tripType;
    private Long startDate;
    private Long pickupTime;
    private Long endDate;
    private Trip.RecurrenceFrequency recurrenceFrequency;
    private Integer recurrenceInterval;
    private String daysOfWeek;
    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String dropoffAddress;
    private BigDecimal dropoffLatitude;
    private BigDecimal dropoffLongitude;
    private TransportRequest.RequestStatus status;

    public static TransportRequestShiftItemDTO fromEntity(TransportRequest request) {
        return fromEntity(request, request.getPickupTime());
    }

    public static TransportRequestShiftItemDTO fromEntity(TransportRequest request, Long pickupTime) {
        return new TransportRequestShiftItemDTO(
                request.getId(),
                request.getPassenger() == null ? null : request.getPassenger().getId(),
                request.getPassengerType(), request.getPassengerName(), request.getPassengerEmail(),
                request.getPassengerPhone(), request.getTripType(), request.getStartDate(), pickupTime, request.getEndDate(),
                request.getRecurrenceFrequency(), request.getRecurrenceInterval(), request.getDaysOfWeek(),
                request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffAddress(), request.getDropoffLatitude(), request.getDropoffLongitude(),
                request.getStatus());
    }
}

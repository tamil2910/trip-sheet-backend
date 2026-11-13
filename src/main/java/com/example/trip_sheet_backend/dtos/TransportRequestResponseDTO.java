package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportRequest;
import com.example.trip_sheet_backend.models.Trip;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransportRequestResponseDTO {
    private UUID id;
    private UUID tenantId;
    private UUID organisationId;
    private UUID shiftId;
    private UUID routeId;
    private UUID requestedByUserId;
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

    public static TransportRequestResponseDTO fromEntity(TransportRequest request) {
        return new TransportRequestResponseDTO(
                request.getId(),
                request.getTenant() == null ? null : request.getTenant().getId(),
                request.getOrganisation() == null ? null : request.getOrganisation().getId(),
                request.getShift() == null ? null : request.getShift().getId(),
                request.getRoute() == null ? null : request.getRoute().getId(),
                request.getRequestedBy() == null ? null : request.getRequestedBy().getId(),
                request.getPassenger() == null ? null : request.getPassenger().getId(),
                request.getPassengerType(), request.getPassengerName(), request.getPassengerEmail(),
                request.getPassengerPhone(), request.getTripType(), request.getStartDate(), request.getPickupTime(), request.getEndDate(),
                request.getRecurrenceFrequency(), request.getRecurrenceInterval(), request.getDaysOfWeek(),
                request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropoffAddress(), request.getDropoffLatitude(), request.getDropoffLongitude(),
                request.getStatus());
    }
}

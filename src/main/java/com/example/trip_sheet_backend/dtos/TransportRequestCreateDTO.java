package com.example.trip_sheet_backend.dtos;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.models.Trip;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransportRequestCreateDTO {
    private String shiftId;

    /** Required when a vendor tenant creates the request. */
    private String organisationId;

    private String passengerType;
    private String passengerId;
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;

    @NotNull(message = "Trip type is required")
    private Trip.TripType tripType;

    /** Epoch seconds, consistent with TripCreateRequestDTO. */
    @NotNull(message = "Start date is required")
    private Long startDate;

    /** Epoch seconds, required for MULTI_DAY and RECURRING. */
    private Long endDate;

    private Trip.RecurrenceFrequency recurrenceFrequency;
    private Integer recurrenceInterval = 1;
    private String daysOfWeek;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal pickupLongitude;

    @NotBlank(message = "Drop-off address is required")
    private String dropoffAddress;

    @NotNull(message = "Drop-off latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal dropoffLatitude;

    @NotNull(message = "Drop-off longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal dropoffLongitude;
}

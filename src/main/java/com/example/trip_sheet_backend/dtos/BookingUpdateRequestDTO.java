package com.example.trip_sheet_backend.dtos;

import java.time.LocalDate;

import com.example.trip_sheet_backend.models.Booking;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingUpdateRequestDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    private Booking.BookingType bookingType;

    private Boolean autoGenerateTrips;

    private String tenantId; // optional corporate update
}

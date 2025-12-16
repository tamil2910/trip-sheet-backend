package com.example.trip_sheet_backend.dtos.TripDtos;

import java.time.LocalDate;
import java.util.List;

import com.example.trip_sheet_backend.models.Booking;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class BookingCreateRequestDTO {

    private String bookingCode;

    private LocalDate startDate;
    private LocalDate endDate;

    private Booking.BookingType bookingType;

    private Boolean autoGenerateTrips;

    // vendor creating booking
    private String vendorId;

    // optional corporate tenant
    private String tenantId;

    // optional child trips (only for SINGLE bookings)
    private List<TripCreateRequestDTO> trips;
}

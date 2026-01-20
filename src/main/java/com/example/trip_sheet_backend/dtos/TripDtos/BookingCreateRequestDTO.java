package com.example.trip_sheet_backend.dtos.TripDtos;

import java.util.List;

import com.example.trip_sheet_backend.models.Booking;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class BookingCreateRequestDTO {

    private String bookingCode;

    private Long startDate;
    private Long endDate;

    private Booking.BookingType bookingType;

    private Boolean autoGenerateTrips;

    // vendor creating booking
    private String vendorId;

    // taken from token
    // private String tenantId; //could be vendor tenant or corporate tenant

    // optional child trips (only for SINGLE bookings)
    private List<TripCreateRequestDTO> trips;
}

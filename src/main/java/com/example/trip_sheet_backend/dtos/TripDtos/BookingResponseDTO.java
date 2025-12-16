package com.example.trip_sheet_backend.dtos.TripDtos;

import java.time.LocalDate;
import java.util.List;

import com.example.trip_sheet_backend.models.Booking;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingResponseDTO {

    private String id;
    private String bookingCode;

    private LocalDate startDate;
    private LocalDate endDate;

    private Booking.BookingType bookingType;

    private Boolean autoGenerateTrips;

    private String vendorId;
    private String vendorName;

    private String tenantId;
    private String tenantName;

    private List<TripResponseDTO> trips;
}

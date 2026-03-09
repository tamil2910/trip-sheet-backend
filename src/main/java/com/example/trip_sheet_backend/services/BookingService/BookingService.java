package com.example.trip_sheet_backend.services.BookingService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.BaseService;
import com.example.trip_sheet_backend.dtos.TripDtos.BookingResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.models.Tenant;

public interface BookingService extends BaseService<Booking, UUID> {
  BookingResponseDTO updateTripInBooking(UUID bookingId, UUID tripId, TripUpdateRequestDTO dto, Tenant tokenTenant, UUID updatedBy);
}

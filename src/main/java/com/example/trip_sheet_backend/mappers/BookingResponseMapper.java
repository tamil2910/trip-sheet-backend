package com.example.trip_sheet_backend.mappers;

import com.example.trip_sheet_backend.dtos.TripDtos.BookingResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopResponseDTO;
import com.example.trip_sheet_backend.models.Booking;
import com.example.trip_sheet_backend.models.Trip;

public class BookingResponseMapper {

    public static BookingResponseDTO toDTO(Booking booking) {

        BookingResponseDTO dto = new BookingResponseDTO();

        dto.setId(booking.getId().toString());
        dto.setBookingCode(booking.getBookingCode());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setBookingType(booking.getBookingType());
        // dto.setAutoGenerateTrips(booking.getAutoGenerateTrips());

        if (booking.getVendor() != null) {
            dto.setVendorId(booking.getVendor().getId().toString());
            dto.setVendorName(booking.getVendor().getTenantName());
        }

        // if (booking.getTenant() != null) {
        //     dto.setTenantId(booking.getTenant().getId().toString());
        //     dto.setTenantName(booking.getTenant().getTenantName());
        // }

        dto.setTrips(
            booking.getTrips().stream()
                .map(BookingResponseMapper::mapTrip)
                .toList()
        );

        return dto;
    }

    private static TripResponseDTO mapTrip(Trip trip) {

        TripResponseDTO dto = new TripResponseDTO();

        dto.setId(trip.getId().toString());
        dto.setTripStatus(trip.getTripStatus());
        dto.setNotes(trip.getNotes());

        if (trip.getVendor() != null) {
            dto.setVendorId(trip.getVendor().getId().toString());
            dto.setVendorName(trip.getVendor().getTenantName());
        }

        if (trip.getOrganisation() != null) {
            dto.setOrganisationId(trip.getOrganisation().getId().toString());
            dto.setOrganisationName(trip.getOrganisation().getTenantName());
        }

        if (trip.getDutyType() != null) {
            dto.setDutyTypeId(trip.getDutyType().getId().toString());
            dto.setDutyTypeName(trip.getDutyType().getName());
        }

        if (trip.getVehicleType() != null) {
            dto.setVehicleTypeId(trip.getVehicleType().getId().toString());
            dto.setVehicleTypeName(trip.getVehicleType().getDefaultName());
        }

        dto.setStops(
            trip.getStops().stream()
                .map(stop -> {
                    TripStopResponseDTO stopDTO = new TripStopResponseDTO();
                    stopDTO.setId(stop.getId().toString());
                    stopDTO.setAddressText(stop.getAddressText());
                    stopDTO.setLatitude(stop.getLatitude());
                    stopDTO.setLongitude(stop.getLongitude());
                    stopDTO.setSequenceNumber(stop.getSequenceNumber());
                    return stopDTO;
                })
                .toList()
        );

        return dto;
    }
}

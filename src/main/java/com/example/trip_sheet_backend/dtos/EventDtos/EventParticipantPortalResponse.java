package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.EventParticipantRequest;
import com.example.trip_sheet_backend.models.Trip;

public record EventParticipantPortalResponse(
    UUID requestId,
    UUID eventId,
    String eventName,
    String participantName,
    String email,
    String phone,
    String pickupAddress,
    Double pickupLatitude,
    Double pickupLongitude,
    String notes,
    EventParticipantRequest.RequestStatus requestStatus,
    TripSummary trip) {

  public static EventParticipantPortalResponse from(EventParticipantRequest request) {
    Trip trip = request.getTrip();
    TripSummary summary = trip == null ? null : new TripSummary(
        trip.getId(), trip.getTripCode(), trip.getTripStatus() == null ? null : trip.getTripStatus().name(), trip.getPickupTime());
    return new EventParticipantPortalResponse(request.getId(), request.getEvent().getId(),
        request.getEvent().getName(), request.getName(), request.getEmail(), request.getPhone(),
        request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude(),
        request.getNotes(), request.getStatus(), summary);
  }

  public record TripSummary(UUID tripId, String tripCode, String status, Long pickupTime) { }
}
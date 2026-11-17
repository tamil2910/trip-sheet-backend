package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.EventParticipantRequest;

public record EventParticipantAdminResponse(
    UUID requestId,
    String name,
    String email,
    String phone,
    String pickupAddress,
    Double pickupLatitude,
    Double pickupLongitude,
    String notes,
    EventParticipantRequest.RequestStatus status,
    EventParticipantRequest.MatchMethod matchMethod,
    UUID peopleTenantId,
    Long submittedAt,
    Long reviewedAt,
    String decisionReason) {
  public static EventParticipantAdminResponse from(EventParticipantRequest request) {
    return new EventParticipantAdminResponse(request.getId(), request.getName(), request.getEmail(), request.getPhone(),
        request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude(), request.getNotes(),
        request.getStatus(), request.getMatchMethod(),
        request.getPeopleTenant() == null ? null : request.getPeopleTenant().getId(),
        request.getCreatedAt(), request.getReviewedAt(), request.getDecisionReason());
  }
}
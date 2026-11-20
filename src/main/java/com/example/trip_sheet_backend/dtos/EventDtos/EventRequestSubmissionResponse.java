package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;
import java.util.List;

import com.example.trip_sheet_backend.models.EventParticipantRequest;

public record EventRequestSubmissionResponse(
    UUID requestId,
    UUID eventId,
    String eventName,
    EventParticipantRequest.RequestStatus status,
    boolean matchedToPeopleTenant,
    String gender,
    List<String> transportTypes,
    List<ExtraPassenger> extraPassengers,
    String message) {
  public record ExtraPassenger(String name, String gender, String relationshipType) { }
}

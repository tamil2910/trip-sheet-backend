package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.EventParticipantRequest;

public record EventRequestSubmissionResponse(
    UUID requestId,
    UUID eventId,
    String eventName,
    EventParticipantRequest.RequestStatus status,
    boolean matchedToPeopleTenant,
    String message) { }
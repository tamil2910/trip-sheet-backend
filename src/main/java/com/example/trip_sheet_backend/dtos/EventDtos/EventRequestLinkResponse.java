package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

public record EventRequestLinkResponse(
    UUID eventId,
    String eventName,
    String venue,
    String city,
    String description,
    boolean bringFamily,
    int allowedExtraPassengers,
    Double venueLatitude,
    Double venueLongitude,
    Long startsAt,
    Long endsAt,
    Long linkValidUntil,
    String invitedName,
    List<EventResponse.VehicleTypeOption> vehicleTypes,
    List<EventResponse.DutyTypeOption> dutyTypes) { }

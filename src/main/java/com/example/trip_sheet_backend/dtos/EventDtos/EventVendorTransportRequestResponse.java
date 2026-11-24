package com.example.trip_sheet_backend.dtos.EventDtos;

import com.example.trip_sheet_backend.models.EventParticipantRequest;
import com.example.trip_sheet_backend.models.EventVendorTransportRequest;
import java.util.List;
import java.util.UUID;

public record EventVendorTransportRequestResponse(
    UUID id, UUID eventId, String eventName, UUID vendorId, String format, String transportType,
    UUID vehicleTypeId, UUID dutyTypeId, Long pickupTime, String status, UUID tripId,
    List<Participant> participants) {
  public record Participant(UUID participantRequestId, String name, String gender, String phone,
      String email, String pickupAddress, Double pickupLatitude, Double pickupLongitude,
      String notes, List<FamilyMember> extraPassengers) { }
  public record FamilyMember(String name, String gender, String relationshipType) { }

  public static EventVendorTransportRequestResponse from(EventVendorTransportRequest request) {
    return new EventVendorTransportRequestResponse(request.getId(), request.getEvent().getId(),
        request.getEvent().getName(), request.getVendor().getId(), request.getFormat().name(),
        request.getTransportType(), request.getVehicleTypeId(), request.getDutyTypeId(),
        request.getPickupTime(), request.getStatus().name(),
        request.getTrip() == null ? null : request.getTrip().getId(),
        request.getParticipants().stream().map(EventVendorTransportRequestResponse::participant).toList());
  }

  private static Participant participant(EventParticipantRequest p) {
    return new Participant(p.getId(), p.getName(), p.getGender(), p.getPhone(), p.getEmail(),
        p.getPickupAddress(), p.getPickupLatitude(), p.getPickupLongitude(), p.getNotes(),
        p.getExtraPassengers().stream().map(x -> new FamilyMember(x.getName(), x.getGender(), x.getRelationshipType())).toList());
  }
}

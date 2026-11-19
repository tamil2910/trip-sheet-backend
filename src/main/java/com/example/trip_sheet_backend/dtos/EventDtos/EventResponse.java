package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;
import com.example.trip_sheet_backend.dtos.EventDtos.EventMutationResponse.RecipientRequestLink;

import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.DutyType;

public record EventResponse(
    UUID id,
    String name,
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
    TransportEvent.RequestAudience requestAudience,
    boolean hasPublicRequestLink,
    String requestLink,
    List<RecipientRequestLink> recipientLinks,
    TransportEvent.EventStatus status,
    List<VehicleTypeOption> vehicleTypes,
    List<DutyTypeOption> dutyTypes) {

  public static EventResponse from(TransportEvent event) {
    return from(event, List.of());
  }

  public static EventResponse from(TransportEvent event, List<RecipientRequestLink> recipientLinks) {
    List<VehicleTypeOption> vehicleTypes = event.getVehicleTypeMappings().stream()
        .map(mapping -> VehicleTypeOption.from(mapping.getVehicleType(), mapping.getAllowedPassenger()))
        .toList();
    List<DutyTypeOption> dutyTypes = event.getDutyTypes().stream().map(DutyTypeOption::from).toList();
    return new EventResponse(event.getId(), event.getName(), event.getVenue(), event.getCity(),
        event.getDescription(), event.isBringFamily(), event.getAllowedExtraPassengers(), event.getVenueLatitude(), event.getVenueLongitude(), event.getStartsAt(), event.getEndsAt(), event.getLinkValidUntil(),
        event.getRequestAudience(), event.getPublicRequestTokenHash() != null, event.getPublicRequestLink(),
        recipientLinks, event.getStatus(), vehicleTypes, dutyTypes);
  }

  public record VehicleTypeOption(UUID id, String name, VehicleType.typeVehicle type, Integer seatCount, int allowedPassenger) {
    public static VehicleTypeOption from(VehicleType vehicleType, int allowedPassenger) {
      return new VehicleTypeOption(vehicleType.getId(), vehicleType.getDefaultName(),
          vehicleType.getTypeOfVehicle(), vehicleType.getSeatCount(), allowedPassenger);
    }
  }

  public record DutyTypeOption(UUID id, String name, DutyType.typeDuty type, Integer km, Integer hr) {
    public static DutyTypeOption from(DutyType dutyType) {
      return new DutyTypeOption(dutyType.getId(), dutyType.getName(), dutyType.getTypeOfDuty(), dutyType.getKm(), dutyType.getHr());
    }
  }
}




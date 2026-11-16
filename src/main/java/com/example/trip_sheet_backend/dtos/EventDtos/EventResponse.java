package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.models.VehicleType;

public record EventResponse(
    UUID id,
    String name,
    String venue,
    String city,
    String description,
    Double venueLatitude,
    Double venueLongitude,
    Long startsAt,
    Long endsAt,
    Long linkValidUntil,
    TransportEvent.RequestAudience requestAudience,
    TransportEvent.EventStatus status,
    List<VehicleTypeOption> vehicleTypes) {

  public static EventResponse from(TransportEvent event) {
    List<VehicleTypeOption> vehicleTypes = event.getVehicleTypes().stream()
        .map(VehicleTypeOption::from)
        .toList();
    return new EventResponse(event.getId(), event.getName(), event.getVenue(), event.getCity(),
        event.getDescription(), event.getVenueLatitude(), event.getVenueLongitude(), event.getStartsAt(), event.getEndsAt(), event.getLinkValidUntil(),
        event.getRequestAudience(), event.getStatus(), vehicleTypes);
  }

  public record VehicleTypeOption(UUID id, String name, VehicleType.typeVehicle type, Integer seatCount) {
    public static VehicleTypeOption from(VehicleType vehicleType) {
      return new VehicleTypeOption(vehicleType.getId(), vehicleType.getDefaultName(),
          vehicleType.getTypeOfVehicle(), vehicleType.getSeatCount());
    }
  }
}



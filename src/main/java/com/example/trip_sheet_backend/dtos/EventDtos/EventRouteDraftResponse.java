package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

public record EventRouteDraftResponse(
    UUID eventId,
    String eventName,
    double maxPickupDistanceKm,
    List<RouteGroup> groups) {

  public record RouteGroup(
      int groupNumber,
      UUID vehicleTypeId,
      String vehicleTypeName,
      int allowedPassenger,
      int passengerCount,
      double maxPickupDistanceKm,
      boolean pickupDistanceExceedsLimit,
      boolean sameRouteGrouping,
      boolean needsEscort,
      List<Passenger> passengers,
      RoutePlan homeToVenue,
      RoutePlan venueToHome,
      String capacityWarning) { }

  public record Passenger(
      UUID participantRequestId,
      String name,
      String gender,
      boolean familyMember,
      String relationshipType,
      String pickupAddress,
      Double pickupLatitude,
      Double pickupLongitude) { }

  public record RoutePlan(
      String direction,
      List<RouteStop> stops,
      double estimatedDistanceKm,
      long estimatedDurationMinutes) { }

  public record RouteStop(String type, String name, String address, double latitude, double longitude) { }
}

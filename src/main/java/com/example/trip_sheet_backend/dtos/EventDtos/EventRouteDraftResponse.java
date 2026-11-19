package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;
import java.util.UUID;

public record EventRouteDraftResponse(
    UUID eventId,
    String eventName,
    double pickupDistanceLimitKm,
    List<RouteGroup> groups) {

  public record RouteGroup(
      int groupNumber,
      Vehicle vehicle,
      int passengerCount,
      double pickupDistanceKm,
      boolean pickupDistanceExceedsLimit,
      boolean sameRouteGrouping,
      boolean needsEscort,
      List<Passenger> passengers,
      Routes routes,
      String capacityWarning) { }

  public record Vehicle(UUID id, String name, int passengerLimit) { }

  public record Routes(RoutePlan homeToVenue, RoutePlan venueToHome) { }

  public record Passenger(
      UUID participantRequestId,
      String name,
      String gender,
      boolean familyMember,
      String relationshipType) { }

  public record RoutePlan(
      List<RouteStop> stops,
      double distanceKm,
      long durationMinutes) { }

  public record RouteStop(String type, String name, String address, double latitude, double longitude) { }
}

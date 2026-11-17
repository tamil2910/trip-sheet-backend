package com.example.trip_sheet_backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.trip_sheet_backend.dtos.EventDtos.EventParticipantAdminResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRouteDraftResponse;
import com.example.trip_sheet_backend.models.EventParticipantFamilyMember;
import com.example.trip_sheet_backend.models.EventParticipantRequest;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.models.TransportEventVehicleType;
import com.example.trip_sheet_backend.repositories.EventParticipantRequestRepository;
import com.example.trip_sheet_backend.repositories.TransportEventRepository;

@Service
public class EventRouteDraftService {
  private static final double DEFAULT_MAX_PICKUP_DISTANCE_KM = 4.0;

  private final TransportEventRepository eventRepository;
  private final EventParticipantRequestRepository participantRepository;
  private final PassengerRouteEstimateService routeEstimateService;

  public EventRouteDraftService(TransportEventRepository eventRepository,
      EventParticipantRequestRepository participantRepository,
      PassengerRouteEstimateService routeEstimateService) {
    this.eventRepository = eventRepository;
    this.participantRepository = participantRepository;
    this.routeEstimateService = routeEstimateService;
  }

  @Transactional(readOnly = true)
  public List<EventParticipantAdminResponse> approvedParticipants(UUID eventId, Tenant tenant) {
    TransportEvent event = getEvent(eventId, tenant);
    return participantRepository.findAllByEvent_IdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
        event.getId(), EventParticipantRequest.RequestStatus.APPROVED).stream()
        .map(EventParticipantAdminResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public EventRouteDraftResponse createDraft(UUID eventId, Tenant tenant, Double requestedMaxPickupDistanceKm) {
    TransportEvent event = getEvent(eventId, tenant);
    if (event.getVenueLatitude() == null || event.getVenueLongitude() == null) {
      throw new IllegalArgumentException("Venue coordinates are required to draft event routes");
    }
    double maxPickupDistanceKm = requestedMaxPickupDistanceKm == null
        ? DEFAULT_MAX_PICKUP_DISTANCE_KM : requestedMaxPickupDistanceKm;
    if (maxPickupDistanceKm <= 0 || maxPickupDistanceKm > DEFAULT_MAX_PICKUP_DISTANCE_KM) {
      throw new IllegalArgumentException("Maximum pickup distance must be greater than 0 and no more than 4 km");
    }

    List<Bundle> bundles = participantRepository
        .findAllByEvent_IdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
            event.getId(), EventParticipantRequest.RequestStatus.APPROVED)
        .stream().map(Bundle::from)
        .sorted(Comparator.comparingDouble((Bundle bundle) -> bundle.latitude())
            .thenComparingDouble(Bundle::longitude)
            .thenComparing(bundle -> bundle.requestId().toString()))
        .toList();

    List<List<Bundle>> routeClusters = clusterByPickupDistance(bundles, maxPickupDistanceKm);
    List<VehicleCapacity> capacities = event.getVehicleTypeMappings().stream()
        .map(mapping -> new VehicleCapacity(mapping.getVehicleType().getId(),
            mapping.getVehicleType().getDefaultName(), mapping.getAllowedPassenger()))
        .filter(vehicle -> vehicle.allowedPassenger() > 0)
        .sorted(Comparator.comparingInt(VehicleCapacity::allowedPassenger))
        .toList();

    List<List<Bundle>> vehicleGroups = splitByVehicleCapacity(routeClusters, capacities);
    List<EventRouteDraftResponse.RouteGroup> groups = new ArrayList<>();
    int groupNumber = 1;
    for (List<Bundle> group : vehicleGroups) {
      groups.add(toRouteGroup(groupNumber++, group, capacities, event));
    }
    return new EventRouteDraftResponse(event.getId(), event.getName(), maxPickupDistanceKm, groups);
  }

  private List<List<Bundle>> clusterByPickupDistance(List<Bundle> bundles, double maxDistanceKm) {
    List<List<Bundle>> clusters = new ArrayList<>();
    for (Bundle bundle : bundles) {
      List<Bundle> bestCluster = null;
      double bestSpread = Double.MAX_VALUE;
      for (List<Bundle> cluster : clusters) {
        double spread = maxPairwiseDistance(cluster, bundle);
        if (spread <= maxDistanceKm && spread < bestSpread) {
          bestCluster = cluster;
          bestSpread = spread;
        }
      }
      if (bestCluster == null) {
        List<Bundle> cluster = new ArrayList<>();
        cluster.add(bundle);
        clusters.add(cluster);
      } else {
        bestCluster.add(bundle);
      }
    }
    return clusters;
  }

  private double maxPairwiseDistance(List<Bundle> group, Bundle candidate) {
    double maxDistance = 0;
    for (Bundle existing : group) {
      maxDistance = Math.max(maxDistance, distanceKm(existing.latitude(), existing.longitude(),
          candidate.latitude(), candidate.longitude()));
    }
    return maxDistance;
  }

  private List<List<Bundle>> splitByVehicleCapacity(List<List<Bundle>> clusters,
      List<VehicleCapacity> capacities) {
    List<List<Bundle>> groups = new ArrayList<>();
    int maximumCapacity = capacities.stream().mapToInt(VehicleCapacity::allowedPassenger).max().orElse(0);
    for (List<Bundle> cluster : clusters) {
      List<Bundle> current = new ArrayList<>();
      int currentPassengerCount = 0;
      for (Bundle bundle : cluster) {
        if (maximumCapacity == 0 || bundle.passengers().size() > maximumCapacity) {
          if (!current.isEmpty()) {
            groups.add(current);
            current = new ArrayList<>();
            currentPassengerCount = 0;
          }
          groups.add(new ArrayList<>(List.of(bundle)));
          continue;
        }
        if (!current.isEmpty() && currentPassengerCount + bundle.passengers().size() > maximumCapacity) {
          groups.add(current);
          current = new ArrayList<>();
          currentPassengerCount = 0;
        }
        current.add(bundle);
        currentPassengerCount += bundle.passengers().size();
      }
      if (!current.isEmpty()) groups.add(current);
    }
    return groups;
  }

  private EventRouteDraftResponse.RouteGroup toRouteGroup(int groupNumber, List<Bundle> bundles,
      List<VehicleCapacity> capacities, TransportEvent event) {
    int passengerCount = bundles.stream().mapToInt(bundle -> bundle.passengers().size()).sum();
    VehicleCapacity vehicle = capacities.stream()
        .filter(candidate -> candidate.allowedPassenger() >= passengerCount)
        .findFirst().orElse(null);
    List<EventRouteDraftResponse.Passenger> passengers = bundles.stream().flatMap(bundle -> bundle.passengers().stream()).toList();
    boolean needsEscort = !passengers.isEmpty() && passengers.stream()
        .allMatch(passenger -> passenger.gender() != null && passenger.gender().equalsIgnoreCase("FEMALE"));
    double maxPickupDistance = maxGroupDistance(bundles);

    Point venue = new Point(event.getVenueLatitude(), event.getVenueLongitude());
    List<Bundle> homeOrder = nearestNeighborOrder(bundles, venue);
    List<Bundle> outboundOrder = new ArrayList<>(homeOrder);
    java.util.Collections.reverse(outboundOrder);
    List<PassengerGeocodingService.GeoPoint> outboundPoints = outboundOrder.stream()
        .map(bundle -> new PassengerGeocodingService.GeoPoint(bundle.latitude(), bundle.longitude())).toList();
    PassengerRouteEstimateService.RouteEstimate outboundEstimate = routeEstimateService.estimate(
        outboundPoints, new PassengerGeocodingService.GeoPoint(venue.latitude(), venue.longitude()));

    List<PassengerGeocodingService.GeoPoint> returnPoints = new ArrayList<>();
    returnPoints.add(new PassengerGeocodingService.GeoPoint(venue.latitude(), venue.longitude()));
    for (int i = 0; i < homeOrder.size() - 1; i++) {
      Bundle stop = homeOrder.get(i);
      returnPoints.add(new PassengerGeocodingService.GeoPoint(stop.latitude(), stop.longitude()));
    }
    Bundle lastHome = homeOrder.get(homeOrder.size() - 1);
    PassengerRouteEstimateService.RouteEstimate returnEstimate = routeEstimateService.estimate(returnPoints,
        new PassengerGeocodingService.GeoPoint(lastHome.latitude(), lastHome.longitude()));

    List<EventRouteDraftResponse.RouteStop> outboundStops = new ArrayList<>();
    for (Bundle bundle : outboundOrder) outboundStops.add(homeStop(bundle));
    outboundStops.add(new EventRouteDraftResponse.RouteStop("EVENT", event.getName(), event.getVenue(),
        venue.latitude(), venue.longitude()));
    List<EventRouteDraftResponse.RouteStop> returnStops = new ArrayList<>();
    returnStops.add(new EventRouteDraftResponse.RouteStop("EVENT", event.getName(), event.getVenue(),
        venue.latitude(), venue.longitude()));
    for (Bundle bundle : homeOrder) returnStops.add(homeStop(bundle));

    return new EventRouteDraftResponse.RouteGroup(groupNumber,
        vehicle == null ? null : vehicle.id(), vehicle == null ? null : vehicle.name(),
        vehicle == null ? 0 : vehicle.allowedPassenger(), passengerCount, maxPickupDistance, needsEscort,
        passengers, new EventRouteDraftResponse.RoutePlan("HOME_TO_EVENT", outboundStops,
            outboundEstimate.distanceKm(), outboundEstimate.durationMinutes()),
        new EventRouteDraftResponse.RoutePlan("EVENT_TO_HOME", returnStops,
            returnEstimate.distanceKm(), returnEstimate.durationMinutes()),
        vehicle == null ? "No configured event vehicle can carry this participant family bundle and passenger count" : null);
  }

  private List<Bundle> nearestNeighborOrder(List<Bundle> bundles, Point start) {
    List<Bundle> remaining = new ArrayList<>(bundles);
    List<Bundle> ordered = new ArrayList<>();
    Point current = start;
    while (!remaining.isEmpty()) {
      Point currentPoint = current;
      Bundle nearest = remaining.stream().min(Comparator.comparingDouble(bundle -> distanceKm(
          currentPoint.latitude(), currentPoint.longitude(), bundle.latitude(), bundle.longitude()))).orElseThrow();
      ordered.add(nearest);
      remaining.remove(nearest);
      current = new Point(nearest.latitude(), nearest.longitude());
    }
    return ordered;
  }

  private EventRouteDraftResponse.RouteStop homeStop(Bundle bundle) {
    return new EventRouteDraftResponse.RouteStop("HOME", bundle.name(), bundle.pickupAddress(),
        bundle.latitude(), bundle.longitude());
  }

  private double maxGroupDistance(List<Bundle> bundles) {
    double maxDistance = 0;
    for (int i = 0; i < bundles.size(); i++) {
      for (int j = i + 1; j < bundles.size(); j++) {
        Bundle first = bundles.get(i);
        Bundle second = bundles.get(j);
        maxDistance = Math.max(maxDistance, distanceKm(first.latitude(), first.longitude(),
            second.latitude(), second.longitude()));
      }
    }
    return round(maxDistance);
  }

  private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
    double latDelta = Math.toRadians(lat2 - lat1);
    double lonDelta = Math.toRadians(lon2 - lon1);
    double a = Math.pow(Math.sin(latDelta / 2), 2) + Math.cos(Math.toRadians(lat1))
        * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(lonDelta / 2), 2);
    return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  private double round(double value) { return Math.round(value * 100.0) / 100.0; }

  private TransportEvent getEvent(UUID eventId, Tenant tenant) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
    return eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(eventId, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
  }

  private record VehicleCapacity(UUID id, String name, int allowedPassenger) { }
  private record Point(double latitude, double longitude) { }
  private record Bundle(UUID requestId, String name, String gender, String pickupAddress,
      double latitude, double longitude, List<EventRouteDraftResponse.Passenger> passengers) {
    static Bundle from(EventParticipantRequest request) {
      List<EventRouteDraftResponse.Passenger> passengers = new ArrayList<>();
      passengers.add(new EventRouteDraftResponse.Passenger(request.getId(), request.getName(), request.getGender(),
          false, null, request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude()));
      for (EventParticipantFamilyMember familyMember : request.getExtraPassengers()) {
        passengers.add(new EventRouteDraftResponse.Passenger(request.getId(), familyMember.getName(), familyMember.getGender(),
            true, familyMember.getRelationshipType(), request.getPickupAddress(), request.getPickupLatitude(), request.getPickupLongitude()));
      }
      return new Bundle(request.getId(), request.getName(), request.getGender(), request.getPickupAddress(),
          request.getPickupLatitude(), request.getPickupLongitude(), passengers);
    }
  }
}


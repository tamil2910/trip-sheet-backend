package com.example.trip_sheet_backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.PassengerRouteDtos;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleRequestDto;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

@Service
public class PassengerRouteService {

    private final PeopleTenantRepository peopleTenantRepository;
    private final PeopleTenantServiceImp peopleTenantService;
    private final PassengerGeocodingService geocodingService;

    public PassengerRouteService(
            PeopleTenantRepository peopleTenantRepository,
            PeopleTenantServiceImp peopleTenantService,
            PassengerGeocodingService geocodingService) {
        this.peopleTenantRepository = peopleTenantRepository;
        this.peopleTenantService = peopleTenantService;
        this.geocodingService = geocodingService;
    }

    @Transactional(readOnly = true)
    public PassengerRouteDtos.Response analyse(PassengerRouteDtos.AnalyseRequest request) {
        requireRequest(request);
        PassengerRouteDtos.Response response = new PassengerRouteDtos.Response(request.getArrivalTime());
        Map<String, List<PassengerRouteDtos.Passenger>> grouped = groupByRoute(request.getPassengers());

        int groupNumber = 1;
        for (List<PassengerRouteDtos.Passenger> passengers : grouped.values()) {
            response.getGroups().add(buildGroup("group-" + groupNumber++, passengers, request.getArrivalTime()));
        }
        return response;
    }

    @Transactional
    public PassengerRouteDtos.Response confirm(
            PassengerRouteDtos.ConfirmationRequest request,
            Tenant organisation,
            UUID createdBy) {
        requireRequest(request);
        if (organisation == null || organisation.getId() == null) {
            throw new IllegalArgumentException("Tenant not found in token");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        PassengerRouteDtos.Response response = new PassengerRouteDtos.Response(request.getArrivalTime());
        int groupNumber = 1;
        for (PassengerRouteDtos.Group submittedGroup : request.getGroups()) {
            if (submittedGroup == null || submittedGroup.getPassengers() == null
                    || submittedGroup.getPassengers().isEmpty()) {
                continue;
            }

            List<PassengerRouteDtos.Passenger> passengers = submittedGroup.getPassengers();
            for (PassengerRouteDtos.Passenger passenger : passengers) {
                resolvePassengerId(passenger, organisation, createdBy);
            }

            String groupId = submittedGroup.getGroupId();
            if (groupId == null || groupId.isBlank()) {
                groupId = "group-" + groupNumber;
            }
            response.getGroups().add(buildGroup(groupId, passengers, request.getArrivalTime()));
            groupNumber++;
        }

        if (request.getUnmatchedPassengers() != null) {
            for (PassengerRouteDtos.Passenger passenger : request.getUnmatchedPassengers()) {
                resolvePassengerId(passenger, organisation, createdBy);
                response.getUnmatchedPassengers().add(passenger);
            }
        }
        return response;
    }

    private PassengerRouteDtos.Group buildGroup(
            String groupId,
            List<PassengerRouteDtos.Passenger> source,
            java.time.Instant requiredArrivalTime) {
        List<PassengerRouteDtos.Passenger> passengers = new ArrayList<>(source);
        passengers.sort(Comparator.comparing(
                PassengerRouteDtos.Passenger::getPickupTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        PassengerRouteDtos.Group group = new PassengerRouteDtos.Group();
        group.setGroupId(groupId);
        group.setPassengers(passengers);
        group.setDestination(passengers.get(0).getDropAddress());
        group.setStartPickupTime(passengers.stream()
                .map(PassengerRouteDtos.Passenger::getPickupTime)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null));
        group.setEstimatedArrivalTime(requiredArrivalTime);
        group.setRouteStatus("READY_FOR_CONFIRMATION");
        return group;
    }

    private void resolvePassengerId(
            PassengerRouteDtos.Passenger passenger,
            Tenant organisation,
            UUID createdBy) {
        if (passenger == null || passenger.getName() == null || passenger.getName().isBlank()) {
            throw new IllegalArgumentException("Passenger name is required");
        }
        if (passenger.getPassengerId() != null) {
            PeopleTenant existing = peopleTenantRepository.findById(passenger.getPassengerId())
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + passenger.getPassengerId()));
            if (existing.getOrganisation() == null
                    || !organisation.getId().equals(existing.getOrganisation().getId())) {
                throw new IllegalArgumentException("Passenger does not belong to this organisation");
            }
            return;
        }

        CreatePeopleRequestDto create = new CreatePeopleRequestDto();
        create.setName(passenger.getName());
        create.setPhone(passenger.getPhone());
        create.setEmail(passenger.getEmail());
        create.setOrganisationId(organisation.getId().toString());
        create.setPeopleType(PeopleTenant.PeopleType.PASSENGER);
        PeopleTenant person = peopleTenantService.createOrGetPerson(create, organisation, createdBy);
        passenger.setPassengerId(person.getId());
    }

    private String routeKey(PassengerRouteDtos.Passenger passenger) {
        return normalize(passenger.getDropAddress());
    }

    private Map<String, List<PassengerRouteDtos.Passenger>> groupByRoute(
            List<PassengerRouteDtos.Passenger> passengers) {
        Map<String, List<PassengerRouteDtos.Passenger>> groups = new HashMap<>();
        Map<String, PassengerGeocodingService.GeoPoint> coordinates = new HashMap<>();

        for (PassengerRouteDtos.Passenger passenger : passengers) {
            PassengerGeocodingService.GeoPoint pickup = geocodePickup(passenger, coordinates);
            PassengerGeocodingService.GeoPoint destination = geocodeDestination(passenger, coordinates);
            double bearing = bearing(pickup, destination);
            String destinationKey = routeKey(passenger);

            List<PassengerRouteDtos.Passenger> matchingGroup = null;
            for (List<PassengerRouteDtos.Passenger> candidate : groups.values()) {
                PassengerRouteDtos.Passenger representative = candidate.get(0);
                if (!destinationKey.equals(routeKey(representative))) {
                    continue;
                }
                PassengerGeocodingService.GeoPoint representativePickup = geocodePickup(representative, coordinates);
                double representativeBearing = bearing(representativePickup,
                        geocodeDestination(representative, coordinates));
                if (distanceKm(pickup, representativePickup) <= 10.0
                        && angularDifference(bearing, representativeBearing) <= 45.0) {
                    matchingGroup = candidate;
                    break;
                }
            }

            if (matchingGroup == null) {
                matchingGroup = new ArrayList<>();
                groups.put(destinationKey + "|" + groups.size(), matchingGroup);
            }
            matchingGroup.add(passenger);
        }
        return groups;
    }

    private PassengerGeocodingService.GeoPoint geocodePickup(
            PassengerRouteDtos.Passenger passenger,
            Map<String, PassengerGeocodingService.GeoPoint> coordinates) {
        if (passenger.getLatitude() != null && passenger.getLongitude() != null) {
            return new PassengerGeocodingService.GeoPoint(passenger.getLatitude(), passenger.getLongitude());
        }
        String query = String.join(", ", nonBlank(passenger.getReportingAddress()),
                nonBlank(passenger.getLocation()), nonBlank(passenger.getCity()));
        return geocode(query, coordinates);
    }

    private PassengerGeocodingService.GeoPoint geocodeDestination(
            PassengerRouteDtos.Passenger passenger,
            Map<String, PassengerGeocodingService.GeoPoint> coordinates) {
        if (passenger.getDestinationLatitude() != null && passenger.getDestinationLongitude() != null) {
            return new PassengerGeocodingService.GeoPoint(
                    passenger.getDestinationLatitude(), passenger.getDestinationLongitude());
        }
        return geocode(String.join(", ", nonBlank(passenger.getDropAddress()), nonBlank(passenger.getCity())), coordinates);
    }

    private PassengerGeocodingService.GeoPoint geocode(
            String query,
            Map<String, PassengerGeocodingService.GeoPoint> coordinates) {
        if (query.isBlank()) {
            throw new IllegalArgumentException("Address, location, and city are required for route analysis");
        }
        return coordinates.computeIfAbsent(normalize(query), geocodingService::geocode);
    }

    private String nonBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private double bearing(PassengerGeocodingService.GeoPoint from, PassengerGeocodingService.GeoPoint to) {
        double latitude1 = Math.toRadians(from.latitude());
        double latitude2 = Math.toRadians(to.latitude());
        double longitudeDifference = Math.toRadians(to.longitude() - from.longitude());
        double y = Math.sin(longitudeDifference) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2)
                - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longitudeDifference);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private double angularDifference(double first, double second) {
        double difference = Math.abs(first - second) % 360;
        return difference > 180 ? 360 - difference : difference;
    }

    private double distanceKm(PassengerGeocodingService.GeoPoint first,
            PassengerGeocodingService.GeoPoint second) {
        double latitudeDifference = Math.toRadians(second.latitude() - first.latitude());
        double longitudeDifference = Math.toRadians(second.longitude() - first.longitude());
        double a = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(Math.toRadians(first.latitude())) * Math.cos(Math.toRadians(second.latitude()))
                * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void requireRequest(Object request) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload is required");
        }
    }
}
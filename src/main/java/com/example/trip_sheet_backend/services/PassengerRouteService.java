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
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

@Service
public class PassengerRouteService {

    private final PeopleTenantRepository peopleTenantRepository;
    private final PeopleTenantServiceImp peopleTenantService;
    private final PassengerGeocodingService geocodingService;
    private final TenantRepository tenantRepository;
    private final PassengerRouteEstimateService routeEstimateService;

    public PassengerRouteService(
            PeopleTenantRepository peopleTenantRepository,
            PeopleTenantServiceImp peopleTenantService,
            PassengerGeocodingService geocodingService,
            TenantRepository tenantRepository,
            PassengerRouteEstimateService routeEstimateService) {
        this.peopleTenantRepository = peopleTenantRepository;
        this.peopleTenantService = peopleTenantService;
        this.geocodingService = geocodingService;
        this.tenantRepository = tenantRepository;
        this.routeEstimateService = routeEstimateService;
    }

    @Transactional(readOnly = true)
    public PassengerRouteDtos.Response analyse(PassengerRouteDtos.AnalyseRequest request) {
        requireRequest(request);
        PassengerRouteDtos.Response response = new PassengerRouteDtos.Response(request.getArrivalTime());
        List<Tenant> organisationMatches = resolveOrganisationMatches(request.getOrganisationId(), request.getCompanyName());
        Tenant resolvedOrganisation = selectOrganisation(organisationMatches, request.getOrganisationId(), request.getCompanyName());
        if (resolvedOrganisation != null) {
            response.setOrganisationId(resolvedOrganisation.getId());
            response.setName(resolvedOrganisation.getTenantName());
        } else {
            response.setOrganisationMatches(organisationMatches.stream()
                    .map(tenant -> new PassengerRouteDtos.OrganisationMatch(tenant.getId(), tenant.getTenantName()))
                    .toList());
        }
        String groupingOrganisationId = resolvedOrganisation != null
                ? resolvedOrganisation.getId().toString()
                : request.getOrganisationId() != null ? request.getOrganisationId() : request.getCompanyName();
        Map<String, List<PassengerRouteDtos.Passenger>> grouped = groupByRoute(
                request.getPassengers(), groupingOrganisationId, request.getCompanyName());

        int groupNumber = 1;
        for (List<PassengerRouteDtos.Passenger> passengers : grouped.values()) {
                PassengerRouteDtos.Group group = buildGroup(
                    "group-" + groupNumber++, passengers, request.getArrivalTime());
                group.setCompanyName(request.getCompanyName());
                if (Boolean.TRUE.equals(request.getIsEstimatedHrKm())) {
                    addRouteEstimates(group);
                }
                response.getGroups().add(group);
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

        Tenant targetOrganisation = resolveOrganisation(request.getOrganisationId(), request.getCompanyName(), organisation);
        PassengerRouteDtos.Response response = new PassengerRouteDtos.Response(request.getArrivalTime());
        response.setOrganisationId(targetOrganisation.getId());
        response.setName(targetOrganisation.getTenantName());
        int groupNumber = 1;
        for (PassengerRouteDtos.Group submittedGroup : request.getGroups()) {
            if (submittedGroup == null || submittedGroup.getPassengers() == null
                    || submittedGroup.getPassengers().isEmpty()) {
                continue;
            }

            List<PassengerRouteDtos.Passenger> passengers = submittedGroup.getPassengers();
            for (PassengerRouteDtos.Passenger passenger : passengers) {
                resolvePassengerId(passenger, targetOrganisation, organisation, createdBy);
            }

            String groupId = submittedGroup.getGroupId();
            if (groupId == null || groupId.isBlank()) {
                groupId = "group-" + groupNumber;
            }
            PassengerRouteDtos.Group group = buildGroup(groupId, passengers, request.getArrivalTime());
            group.setCompanyName(request.getCompanyName());
            response.getGroups().add(group);
            groupNumber++;
        }

        if (request.getUnmatchedPassengers() != null) {
            for (PassengerRouteDtos.Passenger passenger : request.getUnmatchedPassengers()) {
                resolvePassengerId(passenger, targetOrganisation, organisation, createdBy);
                response.getUnmatchedPassengers().add(passenger);
            }
        }
        return response;
    }

    private Tenant resolveOrganisation(String organisationId, String companyName, Tenant authenticatedTenant) {
        if (authenticatedTenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (organisationId != null && !authenticatedTenant.getId().equals(UUID.fromString(organisationId))) {
                throw new IllegalArgumentException("organisationId does not match authenticated organisation");
            }
            return authenticatedTenant;
        }
        if (organisationId == null || organisationId.isBlank()) {
            List<Tenant> matches = resolveOrganisationMatches(null, companyName);
            Tenant selected = selectOrganisation(matches, null, companyName);
            if (selected == null) {
                throw new IllegalArgumentException("Company name is ambiguous or does not match an organisation; select an organisationId");
            }
            return selected;
        }
        Tenant target = tenantRepository.findById(UUID.fromString(organisationId))
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + organisationId));
        if (target.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new IllegalArgumentException("organisationId must reference an organisation tenant");
        }
        return target;
    }

    private List<Tenant> resolveOrganisationMatches(String organisationId, String companyName) {
        if (organisationId != null && !organisationId.isBlank()) {
            Tenant tenant = tenantRepository.findById(UUID.fromString(organisationId))
                    .filter(value -> value.getTenantType() == Tenant.TenantType.ORGANISATION)
                    .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + organisationId));
            return List.of(tenant);
        }
        if (companyName == null || companyName.isBlank()) {
            return List.of();
        }
        List<Tenant> matches = tenantRepository.findByTenantNameContainingIgnoreCase(companyName.trim()).stream()
                .filter(tenant -> tenant.getTenantType() == Tenant.TenantType.ORGANISATION)
                .toList();
        List<Tenant> exactMatches = matches.stream()
                .filter(tenant -> tenant.getTenantName().equalsIgnoreCase(companyName.trim()))
                .toList();
        return exactMatches.isEmpty() ? matches : exactMatches;
    }

    private Tenant selectOrganisation(List<Tenant> matches, String organisationId, String companyName) {
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (organisationId != null && !organisationId.isBlank()) {
            return matches.stream().findFirst().orElse(null);
        }
        if (companyName != null && !companyName.isBlank()) {
            return matches.stream()
                    .filter(tenant -> tenant.getTenantName().equalsIgnoreCase(companyName.trim()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
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

    private void addRouteEstimates(PassengerRouteDtos.Group group) {
        List<PassengerRouteDtos.Passenger> passengers = group.getPassengers();
        List<PassengerGeocodingService.GeoPoint> pickupPoints = passengers.stream()
                .map(passenger -> new PassengerGeocodingService.GeoPoint(
                        passenger.getLatitude(), passenger.getLongitude()))
                .toList();
        PassengerRouteDtos.Passenger finalPassenger = passengers.get(passengers.size() - 1);
        PassengerGeocodingService.GeoPoint destination = new PassengerGeocodingService.GeoPoint(
                finalPassenger.getDestinationLatitude(), finalPassenger.getDestinationLongitude());

        PassengerRouteEstimateService.RouteEstimate estimate = routeEstimateService.estimate(pickupPoints, destination);
        List<PassengerRouteEstimateService.RouteLeg> legs = estimate.legs();

        // The first pickup is the assumed vehicle start point because no vehicle origin is supplied.
        passengers.get(0).setEstimatedKmToPickup(0.0);
        passengers.get(0).setEstimatedMinutesToPickup(0L);
        for (int index = 1; index < passengers.size(); index++) {
            if (index - 1 < legs.size()) {
                PassengerRouteEstimateService.RouteLeg leg = legs.get(index - 1);
                passengers.get(index).setEstimatedKmToPickup(leg.distanceKm());
                passengers.get(index).setEstimatedMinutesToPickup(leg.durationMinutes());
            }
        }
        group.setEstimatedTotalKm(estimate.distanceKm());
        group.setEstimatedTotalMinutes(estimate.durationMinutes());
    }

    private void resolvePassengerId(
            PassengerRouteDtos.Passenger passenger,
            Tenant targetOrganisation,
            Tenant authenticatedTenant,
            UUID createdBy) {
        if (passenger == null || passenger.getName() == null || passenger.getName().isBlank()) {
            throw new IllegalArgumentException("Passenger name is required");
        }
        if (passenger.getPassengerId() != null) {
            PeopleTenant existing = peopleTenantRepository.findById(passenger.getPassengerId())
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + passenger.getPassengerId()));
            if (existing.getOrganisation() == null
                    || !targetOrganisation.getId().equals(existing.getOrganisation().getId())) {
                throw new IllegalArgumentException("Passenger does not belong to this organisation");
            }
                if (authenticatedTenant.getTenantType() == Tenant.TenantType.VENDOR
                    && existing.getAttachedVendors().stream()
                        .noneMatch(vendor -> authenticatedTenant.getId().equals(vendor.getId()))) {
                existing.getAttachedVendors().add(authenticatedTenant);
                peopleTenantRepository.save(existing);
                }
            return;
        }

        CreatePeopleRequestDto create = new CreatePeopleRequestDto();
        create.setName(passenger.getName());
        create.setPhone(passenger.getPhone());
        create.setEmail(passenger.getEmail());
        create.setOrganisationId(targetOrganisation.getId().toString());
        create.setPeopleType(PeopleTenant.PeopleType.PASSENGER);
        PeopleTenant person = peopleTenantService.createOrGetPerson(create, authenticatedTenant, createdBy);
        passenger.setPassengerId(person.getId());
    }

    private String routeKey(PassengerRouteDtos.Passenger passenger) {
        return normalize(passenger.getDropAddress());
    }

        private Map<String, List<PassengerRouteDtos.Passenger>> groupByRoute(
            List<PassengerRouteDtos.Passenger> passengers,
                String organisationId,
            String companyName) {
        Map<String, List<PassengerRouteDtos.Passenger>> groups = new HashMap<>();
        Map<String, PassengerGeocodingService.GeoPoint> coordinates = new HashMap<>();

        for (PassengerRouteDtos.Passenger passenger : passengers) {
            PassengerGeocodingService.GeoPoint pickup = geocodePickup(passenger, coordinates);
            PassengerGeocodingService.GeoPoint destination = geocodeDestination(passenger, coordinates);
            passenger.setLatitude(pickup.latitude());
            passenger.setLongitude(pickup.longitude());
            passenger.setDestinationLatitude(destination.latitude());
            passenger.setDestinationLongitude(destination.longitude());
            double bearing = bearing(pickup, destination);
                String destinationKey = normalize(organisationId) + "|"
                    + normalize(companyName) + "|" + routeKey(passenger);

            List<PassengerRouteDtos.Passenger> matchingGroup = null;
            for (List<PassengerRouteDtos.Passenger> candidate : groups.values()) {
                PassengerRouteDtos.Passenger representative = candidate.get(0);
                String representativeKey = normalize(organisationId) + "|"
                    + normalize(companyName) + "|" + routeKey(representative);
                if (!destinationKey.equals(representativeKey)) {
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
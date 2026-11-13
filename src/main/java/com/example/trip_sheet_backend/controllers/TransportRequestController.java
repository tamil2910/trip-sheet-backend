package com.example.trip_sheet_backend.controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TransportRequestCreateDTO;
import com.example.trip_sheet_backend.dtos.TransportRequestResponseDTO;
import com.example.trip_sheet_backend.dtos.TransportRequestRouteGroupDTO;
import com.example.trip_sheet_backend.dtos.TransportRequestRouteMoveDTO;
import com.example.trip_sheet_backend.dtos.TransportRequestShiftGroupDTO;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Shift;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportRequest;
import com.example.trip_sheet_backend.models.TransportRoute;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.ShiftRepository;
import com.example.trip_sheet_backend.repositories.TransportRequestRepository;
import com.example.trip_sheet_backend.repositories.TransportRouteRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.services.PassengerGeocodingService;
import com.example.trip_sheet_backend.services.PassengerRouteEstimateService;
import com.example.trip_sheet_backend.response_setups.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transport-requests")
@Transactional
public class TransportRequestController {
    private static final Set<String> WEEKDAYS = Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final TransportRequestRepository requestRepository;
    private final ShiftRepository shiftRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final TenantRepository tenantRepository;
    private final VendorOrganisationRepository vendorOrganisationRepository;
    private final TransportRouteRepository routeRepository;
    private final PassengerRouteEstimateService routeEstimateService;

    public TransportRequestController(TransportRequestRepository requestRepository,
            ShiftRepository shiftRepository, PeopleTenantRepository peopleTenantRepository,
            TenantRepository tenantRepository, VendorOrganisationRepository vendorOrganisationRepository,
            TransportRouteRepository routeRepository, PassengerRouteEstimateService routeEstimateService) {
        this.requestRepository = requestRepository;
        this.shiftRepository = shiftRepository;
        this.peopleTenantRepository = peopleTenantRepository;
        this.tenantRepository = tenantRepository;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
        this.routeRepository = routeRepository;
        this.routeEstimateService = routeEstimateService;
    }

    /** Guest employees submit for themselves; passenger identity is taken from their organisation profile. */
    @PostMapping("/my")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<TransportRequestResponseDTO>> createMyRequest(
            @Valid @RequestBody TransportRequestCreateDTO body,
            @RequestParam(required = false) UUID organisationId,
            HttpServletRequest httpRequest) {
        UserAccount user = requireUser(httpRequest);
        Tenant organisation = resolveGuestOrganisation(user, organisationId);
        PeopleTenant employee = peopleTenantRepository
                .findFirstByUserAccount_IdAndOrganisation_Id(user.getId(), organisation.getId())
                .orElseThrow(() -> new RuntimeException("Employee profile not found for this organisation"));
        if (employee.getOrganisationPersonType() != PeopleTenant.OrganisationPersonType.EMPLOYEE) {
            throw new RuntimeException("Only organisation employees can submit employee transport requests");
        }

        TransportRequest request = buildRequest(body, organisation, organisation, user, TransportRequest.PassengerType.EMPLOYEE,
                employee, employee.getName(), employee.getEmail(), employee.getPhone(), true);
        setCreateAudit(request, user);
        return created(saveAndRecalculateRoute(request, null));
    }

    /** Tenant users submit for a passenger; passenger details are supplied by the creator. */
    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_TRANSPORTREQUEST')")
    public ResponseEntity<ApiResponse<TransportRequestResponseDTO>> createForPassenger(
            @Valid @RequestBody TransportRequestCreateDTO body, HttpServletRequest httpRequest) {
        UserAccount user = requireUser(httpRequest);
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        Tenant organisation = resolveTargetOrganisation(body, tenant);

        TransportRequest.PassengerType passengerType = resolvePassengerType(body, organisation);
        PeopleTenant passenger = resolvePassenger(body, organisation, tenant, passengerType, user);
        TransportRequest request = buildRequest(body, tenant, organisation, user, passengerType,
                passenger, passenger.getName(), passenger.getEmail(), passenger.getPhone(), false);
        setCreateAudit(request, user);
        return created(saveAndRecalculateRoute(request, null));
    }

    @PutMapping("/my/{id}")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<TransportRequestResponseDTO>> updateMyRequest(
            @PathVariable UUID id,
            @Valid @RequestBody TransportRequestCreateDTO body,
            @RequestParam(required = false) UUID organisationId,
            HttpServletRequest httpRequest) {
        UserAccount user = requireUser(httpRequest);
        Tenant organisation = resolveGuestOrganisation(user, organisationId);
        TransportRequest request = findPendingRequest(id);
        if (request.getRequestedBy() == null || !user.getId().equals(request.getRequestedBy().getId())) {
            throw new RuntimeException("Transport request does not belong to this guest account");
        }
        if (request.getOrganisation() == null || !organisation.getId().equals(request.getOrganisation().getId())) {
            throw new RuntimeException("Transport request does not belong to this organisation");
        }
        PeopleTenant employee = peopleTenantRepository
                .findFirstByUserAccount_IdAndOrganisation_Id(user.getId(), organisation.getId())
                .orElseThrow(() -> new RuntimeException("Employee profile not found for this organisation"));
        if (employee.getOrganisationPersonType() != PeopleTenant.OrganisationPersonType.EMPLOYEE) {
            throw new RuntimeException("Only organisation employees can update employee transport requests");
        }

        UUID previousRouteId = request.getRoute() == null ? null : request.getRoute().getId();
        applyRequestFields(request, body, organisation, TransportRequest.PassengerType.EMPLOYEE, employee, true);
        request.setUpdatedBy(user.getId().toString());
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport request updated successfully",
                TransportRequestResponseDTO.fromEntity(saveAndRecalculateRoute(request, previousRouteId))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_TRANSPORTREQUEST')")
    public ResponseEntity<ApiResponse<TransportRequestResponseDTO>> updateForPassenger(
            @PathVariable UUID id,
            @Valid @RequestBody TransportRequestCreateDTO body,
            HttpServletRequest httpRequest) {
        UserAccount user = requireUser(httpRequest);
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        TransportRequest request = findPendingRequest(id);
        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (request.getTenant() == null || !tenant.getId().equals(request.getTenant().getId())) {
                throw new RuntimeException("Transport request does not belong to this vendor tenant");
            }
        } else if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (request.getOrganisation() == null || !tenant.getId().equals(request.getOrganisation().getId())) {
                throw new RuntimeException("Transport request does not belong to this organisation");
            }
        } else {
            throw new RuntimeException("Only vendor and organisation tenants can update transport requests");
        }

        Tenant organisation = resolveTargetOrganisation(body, tenant);
        TransportRequest.PassengerType passengerType = resolvePassengerType(body, organisation);
        PeopleTenant passenger = resolvePassenger(body, organisation, tenant, passengerType, user);
        UUID previousRouteId = request.getRoute() == null ? null : request.getRoute().getId();
        applyRequestFields(request, body, organisation, passengerType, passenger, false);
        request.setUpdatedBy(user.getId().toString());
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport request updated successfully",
                TransportRequestResponseDTO.fromEntity(saveAndRecalculateRoute(request, previousRouteId))));
    }

    @PutMapping("/{id}/route")
    @PreAuthorize("hasAuthority('CAN_UPDATE_TRANSPORTREQUEST')")
    public ResponseEntity<ApiResponse<TransportRequestResponseDTO>> moveRequestToRoute(
            @PathVariable UUID id,
            @Valid @RequestBody TransportRequestRouteMoveDTO body,
            HttpServletRequest httpRequest) {
        UserAccount user = requireUser(httpRequest);
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        TransportRequest request = findPendingRequest(id);
        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (request.getTenant() == null || !tenant.getId().equals(request.getTenant().getId())) {
                throw new RuntimeException("Transport request does not belong to this vendor tenant");
            }
        } else if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (request.getOrganisation() == null || !tenant.getId().equals(request.getOrganisation().getId())) {
                throw new RuntimeException("Transport request does not belong to this organisation");
            }
        } else {
            throw new RuntimeException("Only vendor and organisation tenants can move transport requests");
        }

        UUID routeId;
        try {
            routeId = UUID.fromString(body.getRouteId());
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("routeId must be a valid UUID");
        }
        TransportRoute route = routeRepository.findById(routeId)
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getIsDeleted()))
                .filter(candidate -> request.getOrganisation() != null
                        && request.getOrganisation().getId().equals(candidate.getOrganisation().getId()))
                .filter(candidate -> request.getShift() != null
                        && request.getShift().getId().equals(candidate.getShift().getId()))
                .orElseThrow(() -> new RuntimeException("Route not found for this organisation and shift"));

        UUID previousRouteId = request.getRoute() == null ? null : request.getRoute().getId();
        request.setRoute(route);
        request.setUpdatedBy(user.getId().toString());
        return ResponseEntity.ok(new ApiResponse<>(true, "Transport request moved to route successfully",
                TransportRequestResponseDTO.fromEntity(saveAndRecalculateRoute(request, previousRouteId))));
    }

    @GetMapping("/shift-wise")
    @PreAuthorize("hasAuthority('CAN_READ_TRANSPORTREQUEST')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TransportRequestShiftGroupDTO>>> getShiftWiseRequests(
            @RequestParam Long startDate,
            @RequestParam Long endDate,
            HttpServletRequest httpRequest) {
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        if (tenant.getTenantType() != Tenant.TenantType.ORGANISATION
                && tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Only vendor and organisation tenants can view transport requests");
        }
        if (endDate < startDate) {
            throw new RuntimeException("endDate must be greater than or equal to startDate");
        }

        boolean organisationSide = tenant.getTenantType() == Tenant.TenantType.ORGANISATION;
        List<TransportRequest> matches = requestRepository.findShiftWiseInDateRange(
                tenant.getId(), organisationSide, startDate, endDate);
        Map<UUID, List<TransportRequest>> grouped = new LinkedHashMap<>();
        for (TransportRequest request : matches) {
            UUID shiftId = request.getShift() == null ? null : request.getShift().getId();
            grouped.computeIfAbsent(shiftId, ignored -> new ArrayList<>()).add(request);
        }

        List<TransportRequestShiftGroupDTO> result = new ArrayList<>();
        for (List<TransportRequest> requests : grouped.values()) {
            result.add(TransportRequestShiftGroupDTO.fromShiftAndRequests(requests.get(0), requests));
        }
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Shift-wise transport requests fetched successfully", result));
    }

    @GetMapping("/shift-route-wise")
    @PreAuthorize("hasAuthority('CAN_READ_TRANSPORTREQUEST')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<TransportRequestRouteGroupDTO>>> getShiftRouteWiseRequests(
            @RequestParam Long startDate,
            @RequestParam Long endDate,
            HttpServletRequest httpRequest) {
        Tenant tenant = (Tenant) httpRequest.getAttribute("tenant");
        if (tenant == null || tenant.getId() == null) {
            throw new RuntimeException("Tenant not found in token");
        }
        if (tenant.getTenantType() != Tenant.TenantType.ORGANISATION
                && tenant.getTenantType() != Tenant.TenantType.VENDOR) {
            throw new RuntimeException("Only vendor and organisation tenants can view transport requests");
        }
        if (endDate < startDate) {
            throw new RuntimeException("endDate must be greater than or equal to startDate");
        }

        boolean organisationSide = tenant.getTenantType() == Tenant.TenantType.ORGANISATION;
        List<TransportRequest> matches = requestRepository.findShiftWiseInDateRange(
                tenant.getId(), organisationSide, startDate, endDate);
        Map<UUID, List<TransportRequest>> requestsByShift = new LinkedHashMap<>();
        for (TransportRequest request : matches) {
            UUID shiftId = request.getShift() == null ? null : request.getShift().getId();
            requestsByShift.computeIfAbsent(shiftId, ignored -> new ArrayList<>()).add(request);
        }

        List<TransportRequestRouteGroupDTO> result = new ArrayList<>();
        for (List<TransportRequest> shiftRequests : requestsByShift.values()) {
            Map<UUID, List<TransportRequest>> requestsByRoute = new LinkedHashMap<>();
            for (TransportRequest request : shiftRequests) {
                UUID routeId = request.getRoute() == null ? null : request.getRoute().getId();
                requestsByRoute.computeIfAbsent(routeId, ignored -> new ArrayList<>()).add(request);
            }

            List<TransportRequestRouteGroupDTO.RouteGroup> routes = new ArrayList<>();
            for (List<TransportRequest> routeRequests : requestsByRoute.values()) {
                TransportRequest first = routeRequests.get(0);
                TransportRoute route = first.getRoute();
                String pickupAddress = route == null ? first.getPickupAddress() : route.getPickupAddress();
                String dropoffAddress = route == null ? first.getDropoffAddress() : route.getDropoffAddress();
                String direction = pickupAddress + " -> " + dropoffAddress;
                routes.add(new TransportRequestRouteGroupDTO.RouteGroup(
                        route == null ? null : route.getId(),
                        route == null ? null : route.getDirection(),
                        direction,
                        pickupAddress,
                        dropoffAddress,
                        routeRequests.stream().map(com.example.trip_sheet_backend.dtos.TransportRequestShiftItemDTO::fromEntity).toList()));
            }

            TransportRequest first = shiftRequests.get(0);
            result.add(new TransportRequestRouteGroupDTO(
                    first.getShift() == null ? null : first.getShift().getId(),
                    first.getShift() == null ? null : first.getShift().getName(),
                    first.getShift() == null ? null : first.getShift().getStartTime(),
                    first.getShift() == null ? null : first.getShift().getEndTime(),
                    routes));
        }
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Shift and route-wise transport requests fetched successfully", result));
    }

    private String normalizeAddress(String address) {
        return address == null ? "" : address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private TransportRequest buildRequest(TransportRequestCreateDTO body, Tenant tenant, Tenant organisation, UserAccount user,
            TransportRequest.PassengerType passengerType, PeopleTenant passenger, String passengerName, String passengerEmail,
            String passengerPhone, boolean shiftRequired) {
        TransportRequest request = new TransportRequest();
        request.setTenant(tenant);
        request.setOrganisation(organisation);
        request.setRequestedBy(user);
        applyRequestFields(request, body, organisation, passengerType, passenger, shiftRequired);
        request.setPassengerName(passengerName);
        request.setPassengerEmail(passengerEmail);
        request.setPassengerPhone(passengerPhone);
        request.setStatus(TransportRequest.RequestStatus.PENDING);
        return request;
    }

    private void applyRequestFields(TransportRequest request, TransportRequestCreateDTO body, Tenant organisation,
            TransportRequest.PassengerType passengerType, PeopleTenant passenger, boolean shiftRequired) {
        validateSchedule(body);
        request.setPassenger(passenger);
        request.setPassengerType(passengerType);
        request.setPassengerName(passenger.getName());
        request.setPassengerEmail(passenger.getEmail());
        request.setPassengerPhone(passenger.getPhone());
        request.setTripType(body.getTripType());
        request.setStartDate(body.getStartDate());
        request.setEndDate(body.getEndDate());
        request.setRecurrenceFrequency(body.getRecurrenceFrequency());
        request.setRecurrenceInterval(body.getRecurrenceInterval() == null ? 1 : body.getRecurrenceInterval());
        request.setDaysOfWeek(normalizeDays(body.getDaysOfWeek()));
        request.setPickupAddress(body.getPickupAddress().trim());
        request.setPickupLatitude(body.getPickupLatitude());
        request.setPickupLongitude(body.getPickupLongitude());
        request.setDropoffAddress(body.getDropoffAddress().trim());
        request.setDropoffLatitude(body.getDropoffLatitude());
        request.setDropoffLongitude(body.getDropoffLongitude());

        if (body.getShiftId() == null || body.getShiftId().isBlank()) {
            if (shiftRequired) throw new RuntimeException("shiftId is required for an employee request");
            request.setShift(null);
            request.setRoute(null);
            request.setPickupTime(null);
        } else {
            UUID shiftId;
            try {
                shiftId = UUID.fromString(body.getShiftId());
            } catch (IllegalArgumentException exception) {
                throw new RuntimeException("shiftId must be a valid UUID");
            }
            Shift shift = shiftRepository.findByIdAndTenant_IdAndIsDeletedFalse(shiftId, organisation.getId())
                    .orElseThrow(() -> new RuntimeException("Shift not found for this tenant"));
            request.setShift(shift);
            TransportRoute route = resolveOrCreateRoute(organisation, shift, request);
            request.setRoute(route);
            request.setPickupTime(calculatePickupTime(request.getStartDate(), shift,
                    route.getDirection() == TransportRoute.RouteDirection.OFFICE_TO_HOME));
        }
    }

    private TransportRoute resolveOrCreateRoute(Tenant organisation, Shift shift, TransportRequest request) {
        String pickup = normalizeAddress(request.getPickupAddress());
        String dropoff = normalizeAddress(request.getDropoffAddress());
        String officeAddress = normalizeAddress(organisation.getAddress());
        for (TransportRoute route : routeRepository.findByOrganisation_IdAndShift_IdAndIsDeletedFalseOrderByCreatedAtAsc(
                organisation.getId(), shift.getId())) {
            boolean samePickup = pickup.equals(normalizeAddress(route.getPickupAddress()));
            boolean sameDropoff = dropoff.equals(normalizeAddress(route.getDropoffAddress()));
            boolean useRoute = switch (route.getDirection()) {
                case HOME_TO_OFFICE -> sameDropoff;
                case OFFICE_TO_HOME -> samePickup;
                case UNKNOWN -> samePickup || sameDropoff;
            };
            if (!useRoute) continue;

            if (route.getDirection() == TransportRoute.RouteDirection.UNKNOWN) {
                if (samePickup && !sameDropoff) {
                    route.setDirection(TransportRoute.RouteDirection.OFFICE_TO_HOME);
                } else if (sameDropoff && !samePickup) {
                    route.setDirection(TransportRoute.RouteDirection.HOME_TO_OFFICE);
                } else if (pickup.equals(officeAddress)) {
                    route.setDirection(TransportRoute.RouteDirection.OFFICE_TO_HOME);
                } else {
                    route.setDirection(TransportRoute.RouteDirection.HOME_TO_OFFICE);
                }
                route = routeRepository.save(route);
            }
            return route;
        }

        TransportRoute route = new TransportRoute();
        route.setOrganisation(organisation);
        route.setShift(shift);
        route.setPickupAddress(request.getPickupAddress());
        route.setPickupLatitude(request.getPickupLatitude());
        route.setPickupLongitude(request.getPickupLongitude());
        route.setDropoffAddress(request.getDropoffAddress());
        route.setDropoffLatitude(request.getDropoffLatitude());
        route.setDropoffLongitude(request.getDropoffLongitude());
        if (!officeAddress.isBlank() && pickup.equals(officeAddress)) {
            route.setDirection(TransportRoute.RouteDirection.OFFICE_TO_HOME);
        } else if (!officeAddress.isBlank() && dropoff.equals(officeAddress)) {
            route.setDirection(TransportRoute.RouteDirection.HOME_TO_OFFICE);
        } else {
            route.setDirection(TransportRoute.RouteDirection.UNKNOWN);
        }
        return routeRepository.save(route);
    }

    private Long calculatePickupTime(Long dateEpochSeconds, Shift shift, boolean fromOffice) {
        LocalDate date = Instant.ofEpochSecond(dateEpochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
        LocalTime time = LocalTime.parse(fromOffice ? shift.getEndTime() : shift.getStartTime());
        return LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC);
    }

    private TransportRequest saveAndRecalculateRoute(TransportRequest request, UUID previousRouteId) {
        TransportRequest saved = requestRepository.save(request);
        UUID currentRouteId = saved.getRoute() == null ? null : saved.getRoute().getId();
        if (currentRouteId != null) {
            recalculateRoutePickupTimes(saved.getRoute());
        }
        if (previousRouteId != null && !previousRouteId.equals(currentRouteId)) {
            routeRepository.findById(previousRouteId).ifPresent(this::recalculateRoutePickupTimes);
        }
        return saved;
    }

    private void recalculateRoutePickupTimes(TransportRoute route) {
        List<TransportRequest> routeRequests = requestRepository
                .findByRoute_IdAndIsDeletedFalseOrderByCreatedAtAsc(route.getId());
        if (routeRequests.isEmpty()) return;

        if (route.getDirection() == TransportRoute.RouteDirection.OFFICE_TO_HOME) {
            for (TransportRequest request : routeRequests) {
                request.setPickupTime(calculatePickupTime(request.getStartDate(), route.getShift(), true));
            }
            requestRepository.saveAll(routeRequests);
            return;
        }

        PassengerGeocodingService.GeoPoint destination = new PassengerGeocodingService.GeoPoint(
                route.getDropoffLatitude().doubleValue(), route.getDropoffLongitude().doubleValue());
        List<TransportRequest> orderedRequests = routeRequests.stream()
                .sorted((left, right) -> Double.compare(
                        distanceKm(right.getPickupLatitude().doubleValue(), right.getPickupLongitude().doubleValue(), destination),
                        distanceKm(left.getPickupLatitude().doubleValue(), left.getPickupLongitude().doubleValue(), destination)))
                .toList();
        List<PassengerGeocodingService.GeoPoint> pickupPoints = orderedRequests.stream()
                .map(item -> new PassengerGeocodingService.GeoPoint(
                        item.getPickupLatitude().doubleValue(), item.getPickupLongitude().doubleValue()))
                .toList();
        PassengerRouteEstimateService.RouteEstimate estimate = routeEstimateService.estimate(pickupPoints, destination);
        List<PassengerRouteEstimateService.RouteLeg> legs = estimate.legs();
        long remainingMinutes = 0;
        long[] minutesFromPickupToOffice = new long[orderedRequests.size()];
        for (int index = orderedRequests.size() - 1; index >= 0; index--) {
            if (index < legs.size()) remainingMinutes += legs.get(index).durationMinutes();
            minutesFromPickupToOffice[index] = remainingMinutes;
        }
        for (int index = 0; index < orderedRequests.size(); index++) {
            TransportRequest item = orderedRequests.get(index);
            long shiftStart = calculatePickupTime(item.getStartDate(), route.getShift(), false);
            item.setPickupTime(shiftStart - minutesFromPickupToOffice[index] * 60L);
        }
        requestRepository.saveAll(orderedRequests);
    }

    private double distanceKm(double latitude, double longitude, PassengerGeocodingService.GeoPoint destination) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(destination.latitude() - latitude);
        double lngDistance = Math.toRadians(destination.longitude() - longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(destination.latitude()))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Tenant resolveTargetOrganisation(TransportRequestCreateDTO body, Tenant tenant) {
        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (body.getOrganisationId() == null || body.getOrganisationId().isBlank()) {
                throw new RuntimeException("organisationId is required when a vendor creates or updates a transport request");
            }
            UUID organisationId;
            try {
                organisationId = UUID.fromString(body.getOrganisationId());
            } catch (IllegalArgumentException exception) {
                throw new RuntimeException("organisationId must be a valid UUID");
            }
            Tenant organisation = tenantRepository.findById(organisationId)
                    .filter(candidate -> candidate.getTenantType() == Tenant.TenantType.ORGANISATION)
                    .orElseThrow(() -> new RuntimeException("Organisation not found"));
            if (vendorOrganisationRepository.findByVendorAndOrganisation_Id(tenant, organisation.getId()).isEmpty()) {
                throw new RuntimeException("Vendor is not linked with this organisation");
            }
            return organisation;
        }
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            if (body.getOrganisationId() != null && !body.getOrganisationId().isBlank()
                    && !tenant.getId().toString().equals(body.getOrganisationId())) {
                throw new RuntimeException("organisationId must match the authenticated organisation tenant");
            }
            return tenant;
        }
        throw new RuntimeException("Only vendor or organisation tenants can create or update transport requests");
    }

    private TransportRequest findPendingRequest(UUID id) {
        TransportRequest request = requestRepository.findById(id)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("Transport request not found"));
        if (request.getStatus() != TransportRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Only pending transport requests can be updated");
        }
        return request;
    }

    private PeopleTenant resolvePassenger(TransportRequestCreateDTO body, Tenant organisation, Tenant actorTenant,
            TransportRequest.PassengerType passengerType, UserAccount actor) {
        if (body.getPassengerId() != null && !body.getPassengerId().isBlank()) {
            UUID passengerId;
            try {
                passengerId = UUID.fromString(body.getPassengerId());
            } catch (IllegalArgumentException exception) {
                throw new RuntimeException("passengerId must be a valid UUID");
            }
            PeopleTenant passenger = peopleTenantRepository.findById(passengerId)
                    .filter(person -> !Boolean.TRUE.equals(person.getIsDeleted()))
                    .filter(person -> person.getOrganisation() != null
                            && organisation.getId().equals(person.getOrganisation().getId()))
                    .orElseThrow(() -> new RuntimeException("Passenger not found under this organisation"));
            TransportRequest.PassengerType existingType = toPassengerType(passenger.getOrganisationPersonType());
            if (existingType != null && existingType != passengerType) {
                throw new RuntimeException("passengerType does not match the selected passenger");
            }
            return passenger;
        }

        if (body.getPassengerName() == null || body.getPassengerName().isBlank()
                || body.getPassengerPhone() == null || body.getPassengerPhone().isBlank()
                || body.getPassengerEmail() == null || body.getPassengerEmail().isBlank()) {
            throw new RuntimeException("passengerName, passengerPhone, and passengerEmail are required when passengerId is omitted");
        }

        PeopleTenant passenger = peopleTenantRepository
                .findByPhoneAndOrganisation_Id(body.getPassengerPhone().trim(), organisation.getId())
                .orElse(null);
        if (passenger == null) {
            passenger = new PeopleTenant();
            passenger.setOrganisation(organisation);
            passenger.setTenantType(PeopleTenant.PeopleTenantType.ORGANISATION);
            passenger.setPeopleType(PeopleTenant.PeopleType.PASSENGER);
            passenger.setCreatorType(actorTenant.getTenantType() == Tenant.TenantType.VENDOR
                    ? PeopleTenant.CreatorType.VENDOR : PeopleTenant.CreatorType.ORGANISATION);
            passenger.setCreatedBy(actor.getId().toString());
            passenger.setOrganisationPersonType(toOrganisationPersonType(passengerType));
        } else {
            TransportRequest.PassengerType existingType = toPassengerType(passenger.getOrganisationPersonType());
            if (existingType != null && existingType != passengerType) {
                throw new RuntimeException("The phone number belongs to a passenger with a different passengerType");
            }
            if (passenger.getOrganisationPersonType() == null) {
                passenger.setOrganisationPersonType(toOrganisationPersonType(passengerType));
            }
        }
        passenger.setName(body.getPassengerName().trim());
        passenger.setPhone(body.getPassengerPhone().trim());
        passenger.setEmail(body.getPassengerEmail().trim());
        passenger.setUpdatedBy(actor.getId().toString());
        if (actorTenant.getTenantType() == Tenant.TenantType.VENDOR
                && passenger.getAttachedVendors().stream().noneMatch(v -> actorTenant.getId().equals(v.getId()))) {
            passenger.getAttachedVendors().add(actorTenant);
        }
        return peopleTenantRepository.save(passenger);
    }

    private PeopleTenant.OrganisationPersonType toOrganisationPersonType(TransportRequest.PassengerType type) {
        return type == TransportRequest.PassengerType.EMPLOYEE
                ? PeopleTenant.OrganisationPersonType.EMPLOYEE
                : PeopleTenant.OrganisationPersonType.VISITOR;
    }

    private TransportRequest.PassengerType toPassengerType(PeopleTenant.OrganisationPersonType type) {
        if (type == null) return null;
        return type == PeopleTenant.OrganisationPersonType.EMPLOYEE
                ? TransportRequest.PassengerType.EMPLOYEE
                : TransportRequest.PassengerType.VISITOR;
    }

    private void validateSchedule(TransportRequestCreateDTO body) {
        if (body.getStartDate() == null) throw new RuntimeException("startDate is required");
        Trip.TripType type = body.getTripType();
        if (type == Trip.TripType.SINGLE) {
            body.setEndDate(body.getStartDate());
            body.setRecurrenceFrequency(null);
            body.setDaysOfWeek(null);
            body.setRecurrenceInterval(1);
            return;
        }
        if (body.getEndDate() == null || body.getEndDate() < body.getStartDate()) {
            throw new RuntimeException("endDate must be on or after startDate for multi-day or recurring requests");
        }
        if (type == Trip.TripType.MULTI_DAY) {
            body.setRecurrenceFrequency(null);
            body.setDaysOfWeek(null);
            body.setRecurrenceInterval(1);
            return;
        }
        if (type != Trip.TripType.RECURRING || body.getRecurrenceFrequency() == null) {
            throw new RuntimeException("recurrenceFrequency is required for recurring requests");
        }
        if (body.getRecurrenceInterval() != null && body.getRecurrenceInterval() < 1) {
            throw new RuntimeException("recurrenceInterval must be at least 1");
        }
        if (body.getRecurrenceFrequency() == Trip.RecurrenceFrequency.WEEKLY) {
            body.setDaysOfWeek(normalizeDays(body.getDaysOfWeek()));
            if (body.getDaysOfWeek() == null || body.getDaysOfWeek().isBlank()) {
                throw new RuntimeException("daysOfWeek is required for weekly recurring requests");
            }
        } else {
            // Monthly recurrence repeats on the day of month from startDate.
            body.setDaysOfWeek(null);
        }
        LocalDate start = Instant.ofEpochSecond(body.getStartDate()).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = Instant.ofEpochSecond(body.getEndDate()).atZone(ZoneOffset.UTC).toLocalDate();
        if (end.isBefore(start)) throw new RuntimeException("endDate must be on or after startDate");
    }

    private String normalizeDays(String days) {
        if (days == null || days.isBlank()) return null;
        Set<String> normalized = Arrays.stream(days.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        if (normalized.isEmpty() || !WEEKDAYS.containsAll(normalized)) {
            throw new RuntimeException("daysOfWeek must contain valid day codes: MON,TUE,WED,THU,FRI,SAT,SUN");
        }
        return Arrays.stream(new String[] {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"})
                .filter(normalized::contains).collect(Collectors.joining(","));
    }

    private Tenant resolveGuestOrganisation(UserAccount user, UUID organisationId) {
        Tenant tenant = user.getTenant();
        if (organisationId != null) {
            if (tenant != null && !organisationId.equals(tenant.getId())) {
                throw new RuntimeException("Guest account is not linked to the selected organisation");
            }
            return peopleTenantRepository.findFirstByUserAccount_IdAndOrganisation_Id(user.getId(), organisationId)
                    .map(PeopleTenant::getOrganisation)
                    .orElseThrow(() -> new RuntimeException("Guest account is not linked to the selected organisation"));
        }
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("organisationId is required for a guest linked to multiple organisations");
        }
        return tenant;
    }

    private TransportRequest.PassengerType parsePassengerType(String value) {
        if (value == null || value.isBlank()) throw new RuntimeException("passengerType is required");
        try {
            return TransportRequest.PassengerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new RuntimeException("passengerType must be EMPLOYEE or VISITOR");
        }
    }

    private TransportRequest.PassengerType resolvePassengerType(TransportRequestCreateDTO body, Tenant organisation) {
        if (body.getPassengerType() != null && !body.getPassengerType().isBlank()) {
            return parsePassengerType(body.getPassengerType());
        }
        if (body.getPassengerId() != null && !body.getPassengerId().isBlank()) {
            UUID passengerId;
            try {
                passengerId = UUID.fromString(body.getPassengerId());
            } catch (IllegalArgumentException exception) {
                throw new RuntimeException("passengerId must be a valid UUID");
            }
            PeopleTenant passenger = peopleTenantRepository.findById(passengerId)
                    .filter(person -> person.getOrganisation() != null
                            && organisation.getId().equals(person.getOrganisation().getId()))
                    .orElseThrow(() -> new RuntimeException("Passenger not found under this organisation"));
            TransportRequest.PassengerType passengerType = toPassengerType(passenger.getOrganisationPersonType());
            if (passengerType != null) return passengerType;
            throw new RuntimeException("passengerType is required because the selected passenger has no organisation person type");
        }
        throw new RuntimeException("passengerType is required when passengerId is omitted");
    }

    private UserAccount requireUser(HttpServletRequest request) {
        UserAccount user = (UserAccount) request.getAttribute("user");
        if (user == null || user.getId() == null) throw new RuntimeException("Authenticated user not found");
        return user;
    }

    private void setCreateAudit(TransportRequest request, UserAccount user) {
        request.setCreatedBy(user.getId().toString());
        request.setUpdatedBy(user.getId().toString());
    }

    private ResponseEntity<ApiResponse<TransportRequestResponseDTO>> created(TransportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
                "Transport request created successfully", TransportRequestResponseDTO.fromEntity(request)));
    }
}

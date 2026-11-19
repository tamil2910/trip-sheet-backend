package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripDtos.EtsTripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.ManualTripExecuteRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripArrivedRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripAllotRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStopPassengerRequestDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.example.trip_sheet_backend.models.TripStop;

@RestController
@RequestMapping("/ets/trips")
public class EtsTripController {

    private final TripServiceImp tripService;
    private final VendorOrganisationRepository vendorOrganisationRepository;

    public EtsTripController(
            TripServiceImp tripService,
            VendorOrganisationRepository vendorOrganisationRepository) {
        this.tripService = tripService;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> createEtsTrip(
            HttpServletRequest request,
            @Valid @RequestBody EtsTripCreateRequestDTO body) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UUID createdBy = (UUID) request.getAttribute("createdBy");
        if (tenant == null || tenant.getId() == null) {
            throw new IllegalArgumentException("Tenant not found in token");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }
        if (body.getAllowedPassenger() == null || body.getAllowedPassenger() < 1) {
            throw new IllegalArgumentException("allowedPassenger must be greater than zero");
        }
        if (body.getPassengerIds() == null || body.getPassengerIds().isEmpty()) {
            throw new IllegalArgumentException("passengerIds from the route group are required");
        }
        if (body.getPassengerIds().size() > body.getAllowedPassenger()) {
            throw new IllegalArgumentException("Passenger count exceeds allowedPassenger for the selected vehicle type");
        }
        if (body.getStops() == null || body.getStops().isEmpty()) {
            throw new IllegalArgumentException("stops from the route group are required");
        }
        validateEtsPickupAssignments(body);

        applyTenantRelations(body, tenant);
        body.setTripCategory(Trip.TripCategory.ETS);
        // ETS creates the trip first; assignment happens through the later allotment flow.
        body.setDriverId(null);
        body.setVehicleId(null);

        Trip trip = tripService.createTrip(body, tenant, createdBy);
        TripResponseDTO response = TripResponseMapper.toDTO(trip, tenant.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "ETS trip created successfully", response));
    }

    private void validateEtsPickupAssignments(EtsTripCreateRequestDTO body) {
        Set<String> tripPassengers = new HashSet<>(body.getPassengerIds());
        Set<String> assignedPassengers = new HashSet<>();
        for (var stop : body.getStops()) {
            if (stop.getStopType() != TripStop.StopType.PICKUP) continue;
            if (stop.getPassengers() == null || stop.getPassengers().isEmpty()) {
                throw new IllegalArgumentException("Every ETS pickup stop must list its passengers and pickup times");
            }
            for (TripStopPassengerRequestDTO passenger : stop.getPassengers()) {
                if (!tripPassengers.contains(passenger.getPassengerId())) {
                    throw new IllegalArgumentException("Pickup stop passenger must be included in passengerIds");
                }
                if (!assignedPassengers.add(passenger.getPassengerId())) {
                    throw new IllegalArgumentException("Each ETS passenger must have exactly one pickup stop");
                }
            }
        }
        if (!assignedPassengers.equals(tripPassengers)) {
            throw new IllegalArgumentException("Each ETS passenger must be assigned to a pickup stop with a pickupTime");
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> listEtsTrips(
            @RequestParam Map<String, Object> filters,
            Pageable pageable,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        int size = parseInt(filters.get("size"), parseInt(filters.get("limit"), 20));
        int page = parseInt(filters.get("page"), -1);
        if (page < 0) page = Math.max(parseInt(filters.get("skip"), 0) / Math.max(size, 1), 0);
        Sort sort = pageable != null && pageable.getSort().isSorted()
                ? pageable.getSort() : Sort.by(Sort.Direction.ASC, "pickupTime");
        Pageable effectivePageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
        Map<String, Object> searchFilters = new HashMap<>();
        if (filters != null) {
            filters.forEach((key, value) -> {
                if (!List.of("page", "size", "skip", "limit", "sort", "searchValue").contains(key))
                    searchFilters.put(key, value);
            });
        }
        searchFilters.put("tripCategory", Trip.TripCategory.ETS);
        List<String> searchValues = filters == null || filters.get("searchValue") == null
                ? List.of() : List.of(filters.get("searchValue").toString().trim());
        Page<Trip> trips = tripService.searchResourcesWithGlobalSearch(
                tenantId, searchFilters, searchValues, effectivePageable);
        List<TripResponseDTO> data = trips.getContent().stream()
                .map(trip -> TripResponseMapper.toDTO(trip, tenantId))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("page", trips.getNumber());
        result.put("size", trips.getSize());
        result.put("currentPageCount", trips.getNumberOfElements());
        result.put("totalItems", trips.getTotalElements());
        result.put("totalPages", trips.getTotalPages());
        return ResponseEntity.ok(new ApiResponse<>(true, "ETS trips fetched successfully", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> updateEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripUpdateRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        Trip existing = requireEtsTrip(tenantId, id);
        if (existing == null) return notFound();
        Trip updated = tripService.updateTrip(tenantId, (Tenant) request.getAttribute("tenant"), id,
                body, (UUID) request.getAttribute("createdBy"));
        return ResponseEntity.ok(new ApiResponse<>(true, "ETS trip updated successfully",
                TripResponseMapper.toDTO(updated, tenantId)));
    }

    @PutMapping("/mark-manual/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> markEtsTripManual(
            @PathVariable @NotNull UUID id, HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        Trip trip = tripService.markTripAsManual(tenantId, id, (UUID) request.getAttribute("updatedBy"));
        return okTrip("ETS trip marked as manual", trip, tenantId);
    }

    @PutMapping("/allot/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> allotEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripAllotRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try {
            Trip trip = tripService.allotDriverVehicle((Tenant) request.getAttribute("tenant"), tenantId,
                    (UserAccount) request.getAttribute("user"), id, body);
            return okTrip("ETS trip assigned to driver and vehicle", trip, tenantId);
        } catch (RuntimeException ex) { return badRequest(ex); }
    }

    @PutMapping("/dispatch/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> dispatchEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripDispatchRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try {
            Trip trip = tripService.dispatchTrip(tenantId, (Tenant) request.getAttribute("tenant"),
                    (UserAccount) request.getAttribute("user"), id, body);
            return okTrip("ETS trip dispatched successfully", trip, tenantId);
        } catch (RuntimeException ex) { return badRequest(ex); }
    }

    @PutMapping("/arrived/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> arrivedEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripArrivedRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try { return okTrip("ETS trip marked as arrived", tripService.arrivedTrip(tenantId,
                (Tenant) request.getAttribute("tenant"), (UserAccount) request.getAttribute("user"), id, body), tenantId); }
        catch (RuntimeException ex) { return badRequest(ex); }
    }

    @PutMapping("/start/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> startEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripStartRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try { return okTrip("ETS trip started successfully", tripService.startTrip(tenantId,
                (Tenant) request.getAttribute("tenant"), (UserAccount) request.getAttribute("user"), id, body), tenantId); }
        catch (RuntimeException ex) { return badRequest(ex); }
    }

    @PutMapping("/drop/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> closeEtsTrip(
            @PathVariable @NotNull UUID id, @Valid @RequestBody TripDropRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try { return okTrip("ETS trip completed successfully", tripService.dropTrip(tenantId,
                (Tenant) request.getAttribute("tenant"), (UserAccount) request.getAttribute("user"), id, body), tenantId); }
        catch (RuntimeException ex) { return badRequest(ex); }
    }

    @PutMapping("/execute-manual/{id}")
    @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
    public ResponseEntity<ApiResponse<TripResponseDTO>> executeEtsManualTrip(
            @PathVariable @NotNull UUID id, @RequestBody ManualTripExecuteRequestDTO body,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        if (requireEtsTrip(tenantId, id) == null) return notFound();
        try { return okTrip("ETS manual trip executed and completed", tripService.executeManualTrip(tenantId,
                (Tenant) request.getAttribute("tenant"), (UserAccount) request.getAttribute("user"), id, body), tenantId); }
        catch (RuntimeException ex) { return badRequest(ex); }
    }

    private int parseInt(Object value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value.toString()); }
        catch (RuntimeException ex) { return fallback; }
    }

    private Trip requireEtsTrip(UUID tenantId, UUID id) {
        Trip trip = tripService.findByIdResource(tenantId, id);
        return trip == null || Boolean.TRUE.equals(trip.getIsDeleted())
                || trip.getTripCategory() != Trip.TripCategory.ETS ? null : trip;
    }

    private ResponseEntity<ApiResponse<TripResponseDTO>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, "ETS trip not found", null));
    }

    private ResponseEntity<ApiResponse<TripResponseDTO>> okTrip(String message, Trip trip, UUID tenantId) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, TripResponseMapper.toDTO(trip, tenantId)));
    }

    private ResponseEntity<ApiResponse<TripResponseDTO>> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<TripResponseDTO>> getEtsTrip(
            @PathVariable UUID id,
            HttpServletRequest request) {
        UUID tenantId = (UUID) request.getAttribute("tenantId");
        Trip trip = tripService.findByIdResource(tenantId, id);
        if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())
                || trip.getTripCategory() != Trip.TripCategory.ETS) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "ETS trip not found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "ETS trip fetched successfully",
                TripResponseMapper.toDTO(trip, tenantId)));
    }

    private void applyTenantRelations(EtsTripCreateRequestDTO body, Tenant tenant) {
        if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
            String vendorId = body.getVendorId();
            body.setOrganisationId(tenant.getId().toString());
            body.setAssociateCustomerId(null);
            if (vendorId != null && !vendorId.isBlank()) {
                UUID selectedVendorId = UUID.fromString(vendorId);
                VendorOrganisation link = vendorOrganisationRepository
                        .findByOrganisationAndVendor_Id(tenant, selectedVendorId)
                        .filter(this::isActiveVendorOrganisation)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "vendorId is not an active vendor relationship for this organisation"));
                body.setVendorId(link.getVendor().getId().toString());
            } else {
                body.setVendorId(null);
            }
            return;
        }

        if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
            if (body.getOrganisationId() == null || body.getOrganisationId().isBlank()) {
                throw new IllegalArgumentException("organisationId is required when a vendor creates an ETS trip");
            }
            UUID organisationId = UUID.fromString(body.getOrganisationId());
            VendorOrganisation link = vendorOrganisationRepository
                    .findByVendorAndOrganisation_Id(tenant, organisationId)
                    .filter(this::isActiveVendorOrganisation)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "organisationId is not linked to this vendor through an active vendor-organisation relationship"));
            body.setOrganisationId(link.getOrganisation().getId().toString());
            body.setVendorId(tenant.getId().toString());
            body.setAssociateCustomerId(null);
            return;
        }

        throw new IllegalArgumentException("Only organisation and vendor tenants can create ETS trips");
    }

    private boolean isActiveVendorOrganisation(VendorOrganisation link) {
        return link != null
                && !Boolean.TRUE.equals(link.getIsDeleted())
                && !Boolean.FALSE.equals(link.getActive())
                && (link.getContractStatus() == null
                        || link.getContractStatus() == VendorOrganisation.ContractStatus.ACTIVE)
                && link.getVendor() != null
                && link.getOrganisation() != null;
    }
}

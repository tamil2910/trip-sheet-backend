package com.example.trip_sheet_backend.services;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TransportRequestRouteGroupDTO.RouteGroup;
import com.example.trip_sheet_backend.dtos.TransportRequestShiftItemDTO;
import com.example.trip_sheet_backend.dtos.TransportRouteDraftConfirmDTO;
import com.example.trip_sheet_backend.dtos.TransportRouteDraftUpdateDTO;
import com.example.trip_sheet_backend.models.Shift;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportRequest;
import com.example.trip_sheet_backend.models.TransportRouteDraft;
import com.example.trip_sheet_backend.models.TransportRoute;
import com.example.trip_sheet_backend.repositories.TransportRouteDraftRepository;
import com.example.trip_sheet_backend.repositories.TransportRequestRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.dtos.TransportRequestRouteGroupDTO.RouteLengthCategory;
import com.example.trip_sheet_backend.services.PassengerGeocodingService.GeoPoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TransportRouteDraftService {
    private static final long DRAFT_LIFETIME_MILLIS = Duration.ofDays(7).toMillis();

    private final TransportRouteDraftRepository repository;
    private final TransportRequestRepository requestRepository;
    private final ObjectMapper objectMapper;
    private final PassengerRouteEstimateService routeEstimateService;
    private final VendorOrganisationRepository vendorOrganisationRepository;

    public TransportRouteDraftService(TransportRouteDraftRepository repository,
            TransportRequestRepository requestRepository, ObjectMapper objectMapper,
            PassengerRouteEstimateService routeEstimateService,
            VendorOrganisationRepository vendorOrganisationRepository) {
        this.repository = repository;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
        this.routeEstimateService = routeEstimateService;
        this.vendorOrganisationRepository = vendorOrganisationRepository;
    }

    @Transactional
    public List<RouteGroup> confirmGroups(Tenant tenant, TransportRouteDraftConfirmDTO body) {
        validateValidityWindow(body.getValidFrom(), body.getValidTo());
        long expiresAt = System.currentTimeMillis() + DRAFT_LIFETIME_MILLIS;
        List<RouteGroup> savedGroups = new java.util.ArrayList<>();
        for (TransportRouteDraftConfirmDTO.Group group : body.getGroups()) {
            if (group.getRouteDirection() == null
                    || group.getRouteDirection() == com.example.trip_sheet_backend.models.TransportRoute.RouteDirection.UNKNOWN) {
                throw new IllegalArgumentException("Each confirmed group requires HOME_TO_OFFICE or OFFICE_TO_HOME routeDirection");
            }
            if (group.getAllowedPassenger() == null || group.getAllowedPassenger() < group.getRequests().size()) {
                throw new IllegalArgumentException("allowedPassenger must be at least the passenger count in each group");
            }
            TransportRequest firstRequest = findAndValidateRequest(tenant, null, null, group.getRequests().get(0));
            Shift shift = firstRequest.getShift();
            if (body.getShiftId() != null && !body.getShiftId().equals(shift.getId())) {
                throw new IllegalArgumentException("shiftId does not match the confirmed group requests");
            }
            List<TransportRequestShiftItemDTO> requestItems = validateAndMergeRequests(
                    tenant, firstRequest.getOrganisation(), shift, group.getRequests());
            RouteMetrics routeMetrics = calculateRouteMetrics(group.getRouteDirection(), requestItems);

            TransportRouteDraft draft = new TransportRouteDraft();
            draft.setTenant(tenant);
            draft.setOrganisation(firstRequest.getOrganisation());
            draft.setShift(shift);
            if (group.getVendorId() != null && tenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
                throw new IllegalArgumentException("Only an organisation can assign a vendor to a route draft");
            }
            draft.setAssignedVendor(resolveAssignedVendor(firstRequest.getOrganisation(), group.getVendorId()));
            draft.setRouteDirection(group.getRouteDirection());
            draft.setVehicleTypeId(group.getVehicleTypeId());
            draft.setAllowedPassenger(group.getAllowedPassenger());
            draft.setDirection(defaultText(group.getDirection(), group.getPickupAddress() + " -> " + group.getDropoffAddress()));
            draft.setPickupAddress(requireAddress(group.getPickupAddress(), "pickupAddress"));
            draft.setDropoffAddress(requireAddress(group.getDropoffAddress(), "dropoffAddress"));
            draft.setDraftStatus(TransportRouteDraft.DraftStatus.ACTIVE);
            draft.setExpiresAt(expiresAt);
            draft.setValidFrom(body.getValidFrom());
            draft.setValidTo(body.getValidTo());
            draft.setRequestsJson(writeRequests(requestItems));
            applyRouteMetrics(draft, routeMetrics);
            draft = repository.save(draft);

            savedGroups.add(toRouteGroup(draft));
        }
        return savedGroups;
    }

    @Transactional
    public RouteGroup update(UUID tenantId, UUID draftId, TransportRouteDraftUpdateDTO body) {
        expireOverdueDrafts();
        TransportRouteDraft draft = repository.findByIdAndTenant_IdAndIsDeletedFalse(draftId, tenantId)
                .orElse(null);
        if (draft == null) return null;
        if (draft.getDraftStatus() != TransportRouteDraft.DraftStatus.ACTIVE) {
            throw new IllegalStateException("Only active route drafts can be updated");
        }
        validateValidityWindow(body.getValidFrom(), body.getValidTo());
        if (body.getRouteDirection() == null
                || body.getRouteDirection() == com.example.trip_sheet_backend.models.TransportRoute.RouteDirection.UNKNOWN) {
            throw new IllegalArgumentException("routeDirection must be HOME_TO_OFFICE or OFFICE_TO_HOME");
        }
        if (body.getAllowedPassenger() == null || body.getAllowedPassenger() < body.getRequests().size()) {
            throw new IllegalArgumentException("allowedPassenger must be at least the passenger count in the group");
        }
        Tenant actor = draft.getTenant();
        List<TransportRequestShiftItemDTO> requestItems = validateAndMergeRequests(
                actor, draft.getOrganisation(), draft.getShift(), body.getRequests());
        RouteMetrics routeMetrics = calculateRouteMetrics(body.getRouteDirection(), requestItems);
        draft.setRouteDirection(body.getRouteDirection());
        draft.setVehicleTypeId(body.getVehicleTypeId());
        draft.setAllowedPassenger(body.getAllowedPassenger());
        draft.setDirection(defaultText(body.getDirection(), body.getPickupAddress() + " -> " + body.getDropoffAddress()));
        draft.setPickupAddress(requireAddress(body.getPickupAddress(), "pickupAddress"));
        draft.setDropoffAddress(requireAddress(body.getDropoffAddress(), "dropoffAddress"));
        draft.setValidFrom(body.getValidFrom());
        draft.setValidTo(body.getValidTo());
        draft.setRequestsJson(writeRequests(requestItems));
        applyRouteMetrics(draft, routeMetrics);
        return toRouteGroup(repository.save(draft));
    }

    @Transactional
    public List<RouteGroup> list(UUID tenantId, TransportRouteDraft.DraftStatus status) {
        expireOverdueDrafts();
        List<TransportRouteDraft> drafts = status == null
                ? repository.findByTenant_IdAndIsDeletedFalseOrderByCreatedAtDesc(tenantId)
                : repository.findByTenant_IdAndDraftStatusAndIsDeletedFalseOrderByCreatedAtDesc(tenantId, status);
        return drafts.stream().map(this::toRouteGroup).toList();
    }

    @Transactional
    public RouteGroup get(UUID tenantId, UUID draftId) {
        expireOverdueDrafts();
        return repository.findByIdAndTenant_IdAndIsDeletedFalse(draftId, tenantId)
                .map(this::toRouteGroup)
                .orElse(null);
    }

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void expireOverdueDrafts() {
        repository.expireActiveDrafts(TransportRouteDraft.DraftStatus.ACTIVE,
                TransportRouteDraft.DraftStatus.EXPIRED, System.currentTimeMillis());
    }

    private RouteGroup toRouteGroup(TransportRouteDraft draft) {
        if (draft.getTotalRouteKm() == null || draft.getRouteLengthCategory() == null) {
            applyRouteMetrics(draft, calculateRouteMetrics(draft.getRouteDirection(),
                    readRequests(draft.getRequestsJson())));
        }
        RouteGroup group = new RouteGroup(null, draft.getRouteDirection(),
                com.example.trip_sheet_backend.models.TransportRoute.RouteStatus.DRAFT,
                draft.getVehicleTypeId(), draft.getAllowedPassenger(), draft.getDirection(),
                draft.getPickupAddress(), draft.getDropoffAddress(), readRequests(draft.getRequestsJson()));
        group.setDraftId(draft.getId());
        group.setDraftStatus(draft.getDraftStatus().name());
        group.setExpiresAt(draft.getExpiresAt());
        group.setValidFrom(draft.getValidFrom());
        group.setValidTo(draft.getValidTo());
        group.setTotalRouteKm(draft.getTotalRouteKm());
        if (draft.getAssignedVendor() != null) {
            group.setAssignedVendorId(draft.getAssignedVendor().getId());
            group.setAssignedVendorName(draft.getAssignedVendor().getTenantName());
        }
        if (draft.getRouteLengthCategory() != null) {
            group.setRouteLengthCategory(RouteLengthCategory.valueOf(draft.getRouteLengthCategory()));
        }
        return group;
    }

    private Tenant resolveAssignedVendor(Tenant organisation, UUID vendorId) {
        if (vendorId == null) return null;
        return vendorOrganisationRepository.findByOrganisationAndVendor_Id(organisation, vendorId)
                .filter(link -> !Boolean.TRUE.equals(link.getIsDeleted()))
                .map(link -> link.getVendor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "vendorId must belong to a non-deleted vendor-organisation link for this organisation"));
    }

    private RouteMetrics calculateRouteMetrics(TransportRoute.RouteDirection direction,
            List<TransportRequestShiftItemDTO> requests) {
        boolean homeToOffice = direction == TransportRoute.RouteDirection.HOME_TO_OFFICE;
        List<GeoPoint> passengerStops = requests.stream()
                .map(request -> homeToOffice
                        ? point(request.getPickupLatitude(), request.getPickupLongitude())
                        : point(request.getDropoffLatitude(), request.getDropoffLongitude()))
                .toList();
        TransportRequestShiftItemDTO first = requests.get(0);
        GeoPoint office = homeToOffice
                ? point(first.getDropoffLatitude(), first.getDropoffLongitude())
                : point(first.getPickupLatitude(), first.getPickupLongitude());
        PassengerRouteEstimateService.RoutePlan plan = routeEstimateService.optimizeStops(
                passengerStops, office, homeToOffice);
        double totalKm = plan.legs().stream().mapToDouble(PassengerRouteEstimateService.RouteLeg::distanceKm).sum();
        RouteLengthCategory category = totalKm <= 20.0 ? RouteLengthCategory.SHORTEST
                : totalKm <= 30.0 ? RouteLengthCategory.MEDIUM : RouteLengthCategory.LONGEST;
        return new RouteMetrics(Math.round(totalKm * 100.0) / 100.0, category);
    }

    private GeoPoint point(java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        return new GeoPoint(latitude.doubleValue(), longitude.doubleValue());
    }

    private void applyRouteMetrics(TransportRouteDraft draft, RouteMetrics metrics) {
        draft.setTotalRouteKm(metrics.totalKm());
        draft.setRouteLengthCategory(metrics.category().name());
    }

    private record RouteMetrics(double totalKm, RouteLengthCategory category) { }

    private List<TransportRequestShiftItemDTO> validateAndMergeRequests(Tenant tenant, Tenant organisation,
            Shift shift, List<TransportRequestShiftItemDTO> requestedItems) {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        java.util.ArrayList<TransportRequestShiftItemDTO> merged = new java.util.ArrayList<>();
        for (TransportRequestShiftItemDTO item : requestedItems) {
            TransportRequest request = findAndValidateRequest(tenant, organisation, shift, item);
            if (!seen.add(request.getId())) {
                throw new IllegalArgumentException("A transport request can appear only once in a draft group");
            }
            if (item.getPickupTime() == null) {
                throw new IllegalArgumentException("pickupTime is required for each request in a confirmed route draft");
            }
            if (item.getPickupLatitude() == null || item.getPickupLongitude() == null
                    || item.getDropoffLatitude() == null || item.getDropoffLongitude() == null) {
                throw new IllegalArgumentException("Pickup and dropoff coordinates are required for every draft request");
            }
            TransportRequestShiftItemDTO value = TransportRequestShiftItemDTO.fromEntity(request, item.getPickupTime());
            value.setPickupAddress(requireAddress(item.getPickupAddress(), "request pickupAddress"));
            value.setPickupLatitude(item.getPickupLatitude());
            value.setPickupLongitude(item.getPickupLongitude());
            value.setDropoffAddress(requireAddress(item.getDropoffAddress(), "request dropoffAddress"));
            value.setDropoffLatitude(item.getDropoffLatitude());
            value.setDropoffLongitude(item.getDropoffLongitude());
            merged.add(value);
        }
        return merged;
    }

    private TransportRequest findAndValidateRequest(Tenant tenant, Tenant expectedOrganisation,
            Shift expectedShift, TransportRequestShiftItemDTO item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("Each draft passenger must include its transport request id");
        }
        TransportRequest request = requestRepository.findById(item.getId())
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Transport request not found: " + item.getId()));
        boolean belongsToTenant = tenant.getTenantType() == Tenant.TenantType.ORGANISATION
                ? request.getOrganisation() != null && tenant.getId().equals(request.getOrganisation().getId())
                : tenant.getTenantType() == Tenant.TenantType.VENDOR
                        && request.getTenant() != null && tenant.getId().equals(request.getTenant().getId());
        if (!belongsToTenant) throw new IllegalArgumentException("Transport request does not belong to this tenant");
        if (request.getStatus() != TransportRequest.RequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending transport requests can be confirmed in a draft");
        }
        if (request.getShift() == null || request.getOrganisation() == null) {
            throw new IllegalArgumentException("Each draft request must have an organisation and a shift");
        }
        if (expectedOrganisation != null && !expectedOrganisation.getId().equals(request.getOrganisation().getId())) {
            throw new IllegalArgumentException("All requests in a draft group must have the same organisation");
        }
        if (expectedShift != null && !expectedShift.getId().equals(request.getShift().getId())) {
            throw new IllegalArgumentException("All requests in a draft group must have the same shift");
        }
        UUID passengerId = request.getPassenger() == null ? null : request.getPassenger().getId();
        if (item.getPassengerId() != null && !item.getPassengerId().equals(passengerId)) {
            throw new IllegalArgumentException("passengerId does not match the selected transport request");
        }
        return request;
    }

    private void validateValidityWindow(Long validFrom, Long validTo) {
        if (validFrom == null || validTo == null || validFrom < 0 || validTo <= validFrom) {
            throw new IllegalArgumentException("validFrom and validTo must be epoch seconds with validTo after validFrom");
        }
    }

    private String requireAddress(String address, String field) {
        if (address == null || address.isBlank()) throw new IllegalArgumentException(field + " is required");
        return address.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String writeRequests(List<TransportRequestShiftItemDTO> requests) {
        try {
            return objectMapper.writeValueAsString(requests);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize route draft passengers", exception);
        }
    }

    private List<TransportRequestShiftItemDTO> readRequests(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TransportRequestShiftItemDTO>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read saved route draft passengers", exception);
        }
    }
}

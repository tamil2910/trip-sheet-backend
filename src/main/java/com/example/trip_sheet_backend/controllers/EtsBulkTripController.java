package com.example.trip_sheet_backend.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripDtos.EtsBulkTripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.EtsTripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ets")
public class EtsBulkTripController {
    private final TripServiceImp tripService;
    private final EtsTripRequestValidator requestValidator;

    public EtsBulkTripController(TripServiceImp tripService, EtsTripRequestValidator requestValidator) {
        this.tripService = tripService;
        this.requestValidator = requestValidator;
    }

    @PostMapping("/bulk-trips")
    @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
    public ResponseEntity<ApiResponse<List<TripResponseDTO>>> createBulkEtsTrips(
            HttpServletRequest request,
            @Valid @RequestBody EtsBulkTripCreateRequestDTO body) {
        Tenant tenant = (Tenant) request.getAttribute("tenant");
        UUID createdBy = (UUID) request.getAttribute("createdBy");
        if (tenant == null || tenant.getId() == null) {
            throw new IllegalArgumentException("Tenant not found in token");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        List<TripCreateRequestDTO> tripRequests = new ArrayList<>();
        for (EtsTripCreateRequestDTO trip : body.getTrips()) {
            if (trip.getDutyTypeId() == null || trip.getDutyTypeId().isBlank()) {
                trip.setDutyTypeId(body.getDutyTypeId());
            }
            if (trip.getVendorId() == null || trip.getVendorId().isBlank()) {
                trip.setVendorId(body.getVendorId());
            }
            if (trip.getDutyTypeId() == null || trip.getDutyTypeId().isBlank()) {
                throw new IllegalArgumentException("Provide dutyTypeId on the bulk request or on every trip item");
            }
            requestValidator.prepare(trip, tenant);
            tripRequests.add(trip);
        }

        List<Trip> trips = tripService.createBulkTrips(tripRequests, tenant, createdBy);
        List<TripResponseDTO> data = trips.stream()
                .map(trip -> TripResponseMapper.toDTO(trip, tenant.getId()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "ETS trips created successfully", data));
    }
}

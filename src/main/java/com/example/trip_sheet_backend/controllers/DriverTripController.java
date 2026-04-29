package com.example.trip_sheet_backend.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/trips/driver")
public class DriverTripController {

  private final TripServiceImp tripServiceImp;

  public DriverTripController(TripServiceImp tripServiceImp) {
    this.tripServiceImp = tripServiceImp;
  }

  @PreAuthorize("hasAuthority('DRIVER_CREATE_TRIP')")
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<Trip>> createTrip(HttpServletRequest request,
      @Valid @RequestBody TripCreateRequestDTO createTripDto) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (createdBy == null) throw new RuntimeException("Authenticated driver id missing");
    if (tokenTenant == null) throw new RuntimeException("Tenant not found in token");

    if (createTripDto.getTripType() == null || createTripDto.getTripType().name().equals("SINGLE") == false) {
      throw new RuntimeException("Driver can only create SINGLE trips");
    }

    // Force driver as creator and as assigned driver
    createTripDto.setDriverId(createdBy.toString());

    Trip trip = tripServiceImp.createTrip(createTripDto, tokenTenant, createdBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip created successfully", trip));
  }

  @PreAuthorize("hasAuthority('DRIVER_READ_TRIP')")
  @GetMapping
  public ApiResponse<Map<String, Object>> getDriverTrips(@RequestParam Map<String, Object> filters,
      Pageable pageable, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID driverId = (UUID) request.getAttribute("userId");

    if (driverId == null) throw new RuntimeException("Driver id missing");

    Page<Trip> result = tripServiceImp.findByDriverOrCreatedBy(tenantId, driverId, pageable);

    Map<String, Object> response = Map.of(
        "data", result.getContent(),
        "currentPage", result.getNumber(),
        "pageSize", result.getSize(),
        "currentPageCount", result.getNumberOfElements(),
        "totalItems", result.getTotalElements(),
        "totalPages", result.getTotalPages()
    );

    return new ApiResponse<>(true, "Success", response);
  }

  @PreAuthorize("hasAuthority('DRIVER_UPDATE_TRIP')")
  @PutMapping("/{id}")
  public ApiResponse<Trip> updateTrip(@PathVariable UUID id, @Valid @RequestBody TripUpdateRequestDTO payload,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID driverId = (UUID) request.getAttribute("userId");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    if (driverId == null) throw new RuntimeException("Driver id missing");

    Trip existing = tripServiceImp.findByIdResource(tenantId, id);
    if (existing == null) return new ApiResponse<>(false, "Trip not found", null);
    if (!driverId.toString().equals(existing.getCreatedBy())) {
      return new ApiResponse<>(false, "Driver can update only their own trips", null);
    }

    Trip updated = tripServiceImp.updateTrip(tenantId, id, payload, updatedBy);
    return new ApiResponse<>(true, "Trip updated successfully", updated);
  }

  @PreAuthorize("hasAuthority('DRIVER_DISPATCH_TRIP')")
  @PutMapping("/dispatch/{id}")
  public ApiResponse<?> dispatchTrip(@PathVariable UUID id, @Valid @RequestBody TripDispatchRequestDTO dto,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    Trip dispatched = tripServiceImp.dispatchTrip(tenantId, tokenTenant, user, id, dto);
    return new ApiResponse<>(true, "Trip dispatched successfully", dispatched);
  }

  @PreAuthorize("hasAuthority('DRIVER_DISPATCH_TRIP')")
  @PutMapping("/arrived/{id}")
  public ApiResponse<?> arrivedTrip(@PathVariable UUID id, @Valid @RequestBody TripDispatchRequestDTO dto,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    Trip arrived = tripServiceImp.arrivedTrip(tenantId, tokenTenant, user, id, null);
    return new ApiResponse<>(true, "Trip marked arrived", arrived);
  }

  @PreAuthorize("hasAuthority('DRIVER_START_TRIP')")
  @PutMapping("/start/{id}")
  public ApiResponse<?> startTrip(@PathVariable UUID id, @Valid @RequestBody TripStartRequestDTO dto,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    Trip started = tripServiceImp.startTrip(tenantId, tokenTenant, user, id, dto);
    return new ApiResponse<>(true, "Trip started", started);
  }

  @PreAuthorize("hasAuthority('DRIVER_END_TRIP')")
  @PutMapping("/end/{id}")
  public ApiResponse<?> endTrip(@PathVariable UUID id, @Valid @RequestBody TripDropRequestDTO dto,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    Trip ended = tripServiceImp.dropTrip(tenantId, tokenTenant, user, id, dto);
    return new ApiResponse<>(true, "Trip completed", ended);
  }

}
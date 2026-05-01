package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripDtos.TripAllotRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripArrivedRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/trips")
public class TripController {

  private final TripServiceImp tripServiceImp;

  public TripController(TripServiceImp tripServiceImp) {
    this.tripServiceImp = tripServiceImp;
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/create")
  public ResponseEntity<ApiResponse<TripResponseDTO>> createTrip(
      HttpServletRequest request,
      @Valid @RequestBody TripCreateRequestDTO createTripDto
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Trip trip = tripServiceImp.createTrip(createTripDto, tokenTenant, createdBy);
    TripResponseDTO response = TripResponseMapper.toDTO(trip);

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Trip created successfully!", response)
    );
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_TRIP')")
  @PostMapping("/bulk-create")
  @org.springframework.transaction.annotation.Transactional
  public ResponseEntity<ApiResponse<List<TripResponseDTO>>> createBulkTrips(
      HttpServletRequest request,
      @Valid @RequestBody List<TripCreateRequestDTO> createTripDtos
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    List<Trip> trips = tripServiceImp.createBulkTrips(createTripDtos, tokenTenant, createdBy);
    List<TripResponseDTO> response = trips.stream().map(TripResponseMapper::toDTO).toList();

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Bulk trips created successfully!", response)
    );
  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping("/{id}")
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ApiResponse<Trip> getById(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    Trip trip = tripServiceImp.findByIdResource(tenantId, id);
    if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
      return new ApiResponse<>(false, "Trip not found", null);
    }

    TripResponseDTO response = TripResponseMapper.toDTO(trip);
    return (ApiResponse<Trip>) (ApiResponse) new ApiResponse<>(true, "Trip fetched successfully!", response);
  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public ApiResponse<Map<String, Object>> getAll(@RequestParam Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    Integer size = parseInt(filters.get("size"), null);
    if (size == null) {
      size = parseInt(filters.get("limit"), 10);
    }

    Integer page = parseInt(filters.get("page"), null);
    if (page == null) {
      Integer skip = parseInt(filters.get("skip"), 0);
      page = size > 0 ? Math.max(skip / size, 0) : 0;
    }

    Sort sort = Sort.by(Sort.Direction.ASC, "pickupTime");
    if (pageable != null && pageable.getSort().isSorted()) {
      sort = pageable.getSort();
    }

    Pageable effectivePageable = PageRequest.of(
        Math.max(page, 0),
        Math.max(size, 1),
        sort);

    String globalSearch = filters.get("searchValue") != null ? filters.get("searchValue").toString().trim() : null;

    Map<String, Object> effectiveFilters = new java.util.HashMap<>();
    if (filters != null) {
      for (String key : filters.keySet()) {
        if (!key.equals("skip") && !key.equals("limit") && !key.equals("searchValue") &&
            !key.equals("page") && !key.equals("size") && !key.equals("sort")) {
          effectiveFilters.put(key, filters.get(key));
        }
      }
    }

    Page<Trip> result = tripServiceImp.searchResourcesWithGlobalSearch(tenantId, effectiveFilters, globalSearch, effectivePageable);

    List<TripResponseDTO> data = result.getContent().stream()
        .map(TripResponseMapper::toDTO)
        .toList();

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("data", data);
    response.put("currentPage", result.getNumber());
    response.put("pageSize", result.getSize());
    response.put("currentPageCount", result.getNumberOfElements());
    response.put("totalItems", result.getTotalElements());
    response.put("totalPages", result.getTotalPages());
    response.put("isFirst", result.isFirst());
    response.put("isLast", result.isLast());
    response.put("hasNext", result.hasNext());
    response.put("hasPrevious", result.hasPrevious());
    response.put("page", page);
    response.put("size", size);

    return new ApiResponse<>(true, "Trips fetched successfully!", response);
  }

  private Integer parseInt(Object value, Integer defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/{id}")
  @org.springframework.transaction.annotation.Transactional
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ApiResponse<Trip> update(
      @PathVariable @NotNull UUID id,
      @Valid @RequestBody TripUpdateRequestDTO payload,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    Trip existingTrip = tripServiceImp.findByIdResource(tenantId, id);
    if (existingTrip == null || Boolean.TRUE.equals(existingTrip.getIsDeleted())) {
      return new ApiResponse<>(false, "Trip not found", null);
    }

    Trip updatedTrip = tripServiceImp.updateTrip(tenantId, id, payload, updatedBy);

    if (updatedTrip == null) {
      return new ApiResponse<>(false, "Trip updation failed", null);
    }

    TripResponseDTO response = TripResponseMapper.toDTO(updatedTrip);
    return (ApiResponse<Trip>) (ApiResponse) new ApiResponse<>(true, "Trip updated successfully!", response);
  }

  @PreAuthorize("hasAuthority('CAN_DELETE_TRIP')")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable @NotNull UUID id, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID deletedBy = (UUID) request.getAttribute("createdBy");

    Trip trip = tripServiceImp.findByIdResource(tenantId, id);
    if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
      return new ApiResponse<>(false, "Trip not found", null);
    }

    trip.setIsDeleted(true);
    trip.setDeletedAt(Instant.now().toEpochMilli());
    if (deletedBy != null) {
      trip.setDeletedBy(deletedBy.toString());
    }

    tripServiceImp.updateResource(tenantId, id, trip);
    return new ApiResponse<>(true, "Trip deleted successfully!", null);
  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping("/parent-child-trips/{id}")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ApiResponse<List<TripResponseDTO>> getParentAndChildTrips(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    List<Trip> trips = tripServiceImp.getParentAndChildTrips(tenantId, id);
    List<TripResponseDTO> response = trips.stream()
        .map(TripResponseMapper::toDTO)
        .toList();

    return (ApiResponse<List<TripResponseDTO>>) (ApiResponse) new ApiResponse<>(
        true,
        "Parent and child trips fetched successfully!",
        response
    );
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/split/{id}")
  public ResponseEntity<ApiResponse<?>> splitChildTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    try {
      Trip splitTrip = tripServiceImp.splitChildTrip(tenantId, id);
      TripResponseDTO response = TripResponseMapper.toDTO(splitTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip split from parent successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  // @PreAuthorize("hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/dispatch/{id}")
  public ResponseEntity<ApiResponse<?>> dispatchTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripDispatchRequestDTO dispatchData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    try {
      Trip dispatchedTrip = tripServiceImp.dispatchTrip(tenantId, tokenTenant, user, id, dispatchData);
      TripResponseDTO response = TripResponseMapper.toDTO(dispatchedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip dispatched successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  // @PreAuthorize("hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/arrived/{id}")
  public ResponseEntity<ApiResponse<?>> arrivedTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripArrivedRequestDTO arrivedData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    try {
      Trip arrivedTrip = tripServiceImp.arrivedTrip(tenantId, tokenTenant, user, id, arrivedData);
      TripResponseDTO response = TripResponseMapper.toDTO(arrivedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip marked as arrived successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  // @PreAuthorize("hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/start/{id}")
  public ResponseEntity<ApiResponse<?>> startTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripStartRequestDTO startData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    try {
      Trip startedTrip = tripServiceImp.startTrip(tenantId, tokenTenant, user, id, startData);
      TripResponseDTO response = TripResponseMapper.toDTO(startedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip started successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  // @PreAuthorize("hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/drop/{id}")
  public ResponseEntity<ApiResponse<?>> dropTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripDropRequestDTO dropData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    try {
      Trip droppedTrip = tripServiceImp.dropTrip(tenantId, tokenTenant, user, id, dropData);
      TripResponseDTO response = TripResponseMapper.toDTO(droppedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip completed successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PutMapping("/allot/{tripId}")
  public ResponseEntity<ApiResponse<?>> reassignTrip(
      @PathVariable @NotNull UUID tripId,
      HttpServletRequest request,
      @Valid @RequestBody TripAllotRequestDTO allotData
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    UUID tokenTenantId = (UUID) request.getAttribute("tenantId");
    UserAccount user = (UserAccount) request.getAttribute("user");
    try {
      Trip reassignedTrip = tripServiceImp.allotDriverVehicle(tokenTenant, tokenTenantId, user, tripId, allotData);
      TripResponseDTO response = TripResponseMapper.toDTO(reassignedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip reassigned successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }
  

}

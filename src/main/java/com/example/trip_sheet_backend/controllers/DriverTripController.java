package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.DriverRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/trips/driver")
public class DriverTripController {

  private final TripServiceImp tripServiceImp;
  private final DriverRepository driverRepository;

  public DriverTripController(TripServiceImp tripServiceImp, DriverRepository driverRepository) {
    this.tripServiceImp = tripServiceImp;
    this.driverRepository = driverRepository;
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

  @PreAuthorize("hasAuthority('CAN_READ_TRIP') or hasRole('DRIVER')")
  @GetMapping
  public ApiResponse<Map<String, Object>> getDriverTrips(@RequestParam Map<String, Object> filters,
      Pageable pageable, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UserAccount currentUser = (UserAccount) request.getAttribute("user");
    List<String> searchValues = extractSearchValues(filters, request);

    if (currentUser == null || currentUser.getId() == null) {
      throw new RuntimeException("Authenticated driver account missing");
    }

    Driver driver = driverRepository.findByAccount_Id(currentUser.getId())
        .orElseThrow(() -> new RuntimeException("Driver profile not found for authenticated user"));

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

    Page<Trip> result = tripServiceImp.findByDriverOrCreatedBy(tenantId, driver.getId(), filters, searchValues, effectivePageable);

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

    return new ApiResponse<>(true, "Driver trips fetched successfully", response);
  }

  @PreAuthorize("hasAuthority('DRIVER_UPDATE_TRIP')")
  @PutMapping("/{id}")
  public ApiResponse<Trip> updateTrip(@PathVariable UUID id, @Valid @RequestBody TripUpdateRequestDTO payload,
      HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID driverId = (UUID) request.getAttribute("userId");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    if (driverId == null) throw new RuntimeException("Driver id missing");

    Trip existing = tripServiceImp.findByIdResource(tenantId, id);
    if (existing == null) return new ApiResponse<>(false, "Trip not found", null);
    if (!driverId.toString().equals(existing.getCreatedBy())) {
      return new ApiResponse<>(false, "Driver can update only their own trips", null);
    }

    Trip updated = tripServiceImp.updateTrip(tenantId, tokenTenant, id, payload, updatedBy);
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

  private List<String> extractSearchValues(Map<String, Object> filters, HttpServletRequest request) {
    String[] rawValues = request.getParameterValues("searchValue");
    if (rawValues != null && rawValues.length > 0) {
      return java.util.Arrays.stream(rawValues)
          .map(this::sanitizeSearchValue)
          .filter(value -> value != null && !value.isBlank())
          .toList();
    }

    if (filters == null || filters.get("searchValue") == null) {
      return List.of();
    }

    String sanitizedValue = sanitizeSearchValue(filters.get("searchValue").toString());
    return sanitizedValue == null || sanitizedValue.isBlank() ? List.of() : List.of(sanitizedValue);
  }

  private String sanitizeSearchValue(String value) {
    if (value == null) {
      return null;
    }

    String trimmedValue = value.trim();
    if (trimmedValue.length() >= 2 && trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) {
      return trimmedValue.substring(1, trimmedValue.length() - 1).trim();
    }

    return trimmedValue;
  }

}

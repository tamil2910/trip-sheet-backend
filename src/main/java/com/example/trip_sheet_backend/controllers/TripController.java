package com.example.trip_sheet_backend.controllers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.trip_sheet_backend.dtos.TripDtos.TripAllotRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripBasicRelationResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripArrivedRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDispatchResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripDropRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.ManualTripExecuteRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripPartnerVendorAssignRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripOrganisationVendorAssignRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripRelationResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripStartRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripSummaryResponseDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripUpdateRequestDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.mappers.TripSummaryResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.security.JwtTokenUtil;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/trips")
public class TripController {
  private static final Logger log = LoggerFactory.getLogger(TripController.class);
  private static final int MAX_DATE_RANGE_SNAPSHOT_TRIPS = 500;

  private final TripServiceImp tripServiceImp;
  private final JwtTokenUtil jwtTokenUtil;
  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  public TripController(TripServiceImp tripServiceImp, JwtTokenUtil jwtTokenUtil,
      SimpMessagingTemplate messagingTemplate, RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper) {
    this.tripServiceImp = tripServiceImp;
    this.jwtTokenUtil = jwtTokenUtil;
    this.messagingTemplate = messagingTemplate;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
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
  @Transactional
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
  @GetMapping("/summary/{tripSummaryId}")
  @Transactional(readOnly = true)
  public ResponseEntity<ApiResponse<TripSummaryResponseDTO>> getSummaryById(
      @PathVariable @NotNull UUID tripSummaryId,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    TripSummary summary = tripServiceImp.findTripSummaryByIdResource(tenantId, tripSummaryId);
    if (summary == null) {
      return ResponseEntity.ok(new ApiResponse<>(false, "Trip summary not found", null));
    }
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip summary fetched successfully!", TripSummaryResponseMapper.toDTO(summary)));
  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<ApiResponse<TripResponseDTO>> getById(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    Trip trip = tripServiceImp.findByIdResource(tenantId, id);
    if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
      return ResponseEntity.ok(new ApiResponse<>(false, "Trip not found", null));
    }

    TripResponseDTO response = TripResponseMapper.toDTO(trip);
    // return (ApiResponse<Trip>) (ApiResponse) new ApiResponse<>(true, "Trip fetched successfully!", response);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip fetched successfully!", response));

  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(@RequestParam Map<String, Object> filters,
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

    List<String> globalSearchValues = extractSearchValues(filters, request);

    Map<String, Object> effectiveFilters = new java.util.HashMap<>();
    if (filters != null) {
      for (String key : filters.keySet()) {
        if (!key.equals("skip") && !key.equals("limit") && !key.equals("searchValue") &&
            !key.equals("page") && !key.equals("size") && !key.equals("sort")) {
          effectiveFilters.put(key, filters.get(key));
        }
      }
    }

    ResponseEntity<ApiResponse<Map<String, Object>>> dateRangeCacheResponse = tryDateRangeSnapshot(
        tenantId, effectiveFilters, globalSearchValues, effectivePageable);
    if (dateRangeCacheResponse != null) {
      return dateRangeCacheResponse;
    }

    String cacheKey = null;
    String cacheStatus = "BYPASS";
    if (tenantId != null) {
      try {
        String generationKey = "trip-list:version:" + tenantId;
        Long generation = redisTemplate.opsForValue().increment(generationKey, 0L);
        cacheKey = createTripSearchCacheKey(tenantId, generation, effectiveFilters, globalSearchValues, effectivePageable);
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        cacheStatus = "MISS";
        if (cachedJson != null) {
          try {
            Map<String, Object> cachedResponse = objectMapper.readValue(cachedJson, new TypeReference<>() {});
            return ResponseEntity.ok()
                .header("X-Trip-Cache", "HIT")
                .body(new ApiResponse<>(true, "Trips fetched successfully!", cachedResponse));
          } catch (JsonProcessingException ex) {
            log.warn("Ignoring invalid cached trip search response for tenant {}", tenantId, ex);
          }
        }
      } catch (DataAccessException ex) {
        cacheKey = null;
        cacheStatus = "BYPASS";
        log.warn("Redis unavailable for trip search; querying database directly", ex);
      }
    }

    Page<Trip> result = tripServiceImp.searchResourcesWithGlobalSearch(tenantId, effectiveFilters, globalSearchValues, effectivePageable);

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

    if (cacheKey != null) {
      try {
        String json = objectMapper.writeValueAsString(response);
        redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
      } catch (JsonProcessingException ex) {
        log.warn("Unable to serialize trip search response for Redis cache", ex);
      } catch (DataAccessException ex) {
        cacheStatus = "BYPASS";
        log.warn("Unable to write trip search response to Redis", ex);
      }
    }

    return ResponseEntity.ok()
        .header("X-Trip-Cache", cacheStatus)
        .body(new ApiResponse<>(true, "Trips fetched successfully!", response));
  }

  private ResponseEntity<ApiResponse<Map<String, Object>>> tryDateRangeSnapshot(
      UUID tenantId,
      Map<String, Object> filters,
      List<String> searchValues,
      Pageable pageable
  ) {
    if (tenantId == null || !filters.containsKey("startDate") || !filters.containsKey("endDate")
        || filters.keySet().stream().anyMatch(key -> !key.equals("startDate") && !key.equals("endDate") && !key.equals("status"))
        || pageable.getSort().stream().anyMatch(order -> !order.getProperty().equals("pickupTime"))) {
      return null;
    }

    Long startDate = parseLongFilter(filters.get("startDate"));
    Long endDate = parseLongFilter(filters.get("endDate"));
    if (startDate == null || endDate == null || startDate > endDate) {
      return null;
    }

    String statusFilter = filters.get("status") == null ? null : filters.get("status").toString().trim();
    Trip.TripStatus requestedStatus = null;
    if (statusFilter != null && !statusFilter.isBlank()) {
      try {
        requestedStatus = statusFilter.equalsIgnoreCase("active")
            ? Trip.TripStatus.STARTED
            : Trip.TripStatus.valueOf(statusFilter.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }

    String cacheKey;
    try {
      Long generation = redisTemplate.opsForValue().increment("trip-list:version:" + tenantId, 0L);
      cacheKey = "trip-range:v1:" + tenantId + ":" + generation + ":" + startDate + ":" + endDate;
      String cachedJson = redisTemplate.opsForValue().get(cacheKey);
      if (cachedJson != null) {
        if (cachedJson.equals("!OVERSIZE!")) {
          return null;
        }
        List<TripResponseDTO> cachedTrips = objectMapper.readValue(cachedJson, new TypeReference<>() {});
        return buildDateRangeResponse(cachedTrips, searchValues, requestedStatus, pageable, "HIT");
      }
    } catch (DataAccessException | JsonProcessingException ex) {
      log.warn("Unable to read trip date-range snapshot from Redis", ex);
      return null;
    }

    Map<String, Object> dateFilters = Map.of("startDate", startDate, "endDate", endDate);
    Pageable snapshotPageable = PageRequest.of(
        0,
        MAX_DATE_RANGE_SNAPSHOT_TRIPS + 1,
        Sort.by(Sort.Direction.ASC, "pickupTime"));
    Page<Trip> snapshotPage = tripServiceImp.searchResourcesWithGlobalSearch(
        tenantId, dateFilters, List.of(), snapshotPageable);

    if (snapshotPage.getTotalElements() > MAX_DATE_RANGE_SNAPSHOT_TRIPS
        || snapshotPage.getContent().size() > MAX_DATE_RANGE_SNAPSHOT_TRIPS) {
      try {
        redisTemplate.opsForValue().set(cacheKey, "!OVERSIZE!", Duration.ofMinutes(10));
      } catch (DataAccessException ex) {
        log.warn("Unable to mark oversized trip date-range snapshot in Redis", ex);
      }
      return null;
    }

    List<TripResponseDTO> trips = snapshotPage.getContent().stream()
        .map(TripResponseMapper::toDTO)
        .toList();
    try {
      redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(trips), Duration.ofMinutes(10));
    } catch (DataAccessException | JsonProcessingException ex) {
      log.warn("Unable to write trip date-range snapshot to Redis", ex);
    }
    return buildDateRangeResponse(trips, searchValues, requestedStatus, pageable, "MISS");
  }

  private ResponseEntity<ApiResponse<Map<String, Object>>> buildDateRangeResponse(
      List<TripResponseDTO> cachedTrips,
      List<String> searchValues,
      Trip.TripStatus requestedStatus,
      Pageable pageable,
      String cacheStatus
  ) {
    List<TripResponseDTO> filteredTrips = cachedTrips.stream()
        .filter(trip -> requestedStatus == null || trip.getTripStatus() == requestedStatus)
        .filter(trip -> matchesSearchValues(trip, searchValues))
        .sorted((left, right) -> {
          Long leftPickup = left.getPickupTime();
          Long rightPickup = right.getPickupTime();
          int comparison = leftPickup == null
              ? (rightPickup == null ? 0 : 1)
              : (rightPickup == null ? -1 : leftPickup.compareTo(rightPickup));
          return pageable.getSort().stream().findFirst()
              .filter(order -> order.getDirection() == Sort.Direction.DESC)
              .map(order -> -comparison)
              .orElse(comparison);
        })
        .toList();

    int page = pageable.getPageNumber();
    int size = pageable.getPageSize();
    long totalItems = filteredTrips.size();
    int totalPages = (int) Math.ceil((double) totalItems / size);
    int fromIndex = (int) Math.min((long) page * size, totalItems);
    int toIndex = Math.min(fromIndex + size, filteredTrips.size());
    List<TripResponseDTO> pageData = filteredTrips.subList(fromIndex, toIndex);

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("data", pageData);
    response.put("currentPage", page);
    response.put("pageSize", size);
    response.put("currentPageCount", pageData.size());
    response.put("totalItems", totalItems);
    response.put("totalPages", totalPages);
    response.put("isFirst", page == 0);
    response.put("isLast", totalPages == 0 || page >= totalPages - 1);
    response.put("hasNext", page + 1 < totalPages);
    response.put("hasPrevious", page > 0 && totalItems > 0);
    response.put("page", page);
    response.put("size", size);

    return ResponseEntity.ok()
        .header("X-Trip-Cache", cacheStatus)
        .body(new ApiResponse<>(true, "Trips fetched successfully!", response));
  }

  private boolean matchesSearchValues(TripResponseDTO trip, List<String> searchValues) {
    if (searchValues == null || searchValues.isEmpty()) {
      return true;
    }

    List<String> searchableFields = new ArrayList<>();
    searchableFields.add(trip.getTripCode());
    searchableFields.add(trip.getNotes());
    addSearchableName(searchableFields, trip.getVendor());
    addSearchableName(searchableFields, trip.getOrganisation());
    addSearchableName(searchableFields, trip.getAssignedByVendor());
    addSearchableName(searchableFields, trip.getPreviousVendor());
    addSearchableName(searchableFields, trip.getDriver());
    addSearchableName(searchableFields, trip.getVehicle());
    addSearchableName(searchableFields, trip.getBooker());
    addSearchableName(searchableFields, trip.getDutyType());
    addSearchableName(searchableFields, trip.getVehicleType());
    if (trip.getPassengers() != null) {
      trip.getPassengers().forEach(passenger -> addSearchableName(searchableFields, passenger));
    }

    return searchValues.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(term -> searchableFields.stream()
            .filter(value -> value != null)
            .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(term)));
  }

  private void addSearchableName(List<String> values, TripRelationResponseDTO relation) {
    if (relation != null) {
      values.add(relation.getName());
    }
  }

  private void addSearchableName(List<String> values, TripBasicRelationResponseDTO relation) {
    if (relation != null) {
      values.add(relation.getName());
    }
  }

  private String createTripSearchCacheKey(UUID tenantId, Long generation, Map<String, Object> filters,
      List<String> searchValues, Pageable pageable) {
    Map<String, Object> keyParts = new LinkedHashMap<>();
    keyParts.put("filters", new TreeMap<>(filters));
    keyParts.put("searchValues", searchValues);
    keyParts.put("page", pageable.getPageNumber());
    keyParts.put("size", pageable.getPageSize());
    keyParts.put("sort", pageable.getSort().toString());

    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(keyParts));
      return "trip-list:v1:" + tenantId + ":" + generation + ":" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
      throw new IllegalStateException("Unable to create trip search cache key", ex);
    }
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

  private Long parseLongFilter(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException ex) {
      return null;
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

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/{id}")
  @Transactional
  public ResponseEntity<ApiResponse<TripResponseDTO>> update(
      @PathVariable @NotNull UUID id,
      @Valid @RequestBody TripUpdateRequestDTO payload,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    Trip existingTrip = tripServiceImp.findByIdResource(tenantId, id);
    if (existingTrip == null || Boolean.TRUE.equals(existingTrip.getIsDeleted())) {
      return ResponseEntity.status(404).body(new ApiResponse<>(false, "Trip not found", null));
    }

    Trip updatedTrip = tripServiceImp.updateTrip(tenantId, tokenTenant, id, payload, updatedBy);

    if (updatedTrip == null) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, "Trip updation failed", null));
    }

    TripResponseDTO response = TripResponseMapper.toDTO(updatedTrip);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip updated successfully!", response));
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/mark-manual/{id}")
  public ResponseEntity<ApiResponse<TripResponseDTO>> markAsManualTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    try {
      Trip updatedTrip = tripServiceImp.markTripAsManual(tenantId, id, updatedBy);
      return ResponseEntity.ok(new ApiResponse<>(
          true,
          "Trip marked as manual successfully!",
          TripResponseMapper.toDTO(updatedTrip)
      ));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasAuthority('CAN_DELETE_TRIP')")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @NotNull UUID id, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID deletedBy = (UUID) request.getAttribute("createdBy");

    Trip trip = tripServiceImp.findByIdResource(tenantId, id);
    if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
      return ResponseEntity.status(404).body(new ApiResponse<>(false, "Trip not found", null));
    }

    tripServiceImp.deleteTrip(tenantId, id, deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip deleted successfully!", null));
  }

  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping("/parent-child-trips/{id}")
  
  public ResponseEntity<ApiResponse<List<TripResponseDTO>>> getParentAndChildTrips(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    List<Trip> trips = tripServiceImp.getParentAndChildTrips(tenantId, id);
    List<TripResponseDTO> response = trips.stream()
        .map(TripResponseMapper::toDTO)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Parent and child trips fetched successfully!",
        response
    ));
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
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/dispatch/{id}")
  public ResponseEntity<ApiResponse<?>> dispatchTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripDispatchRequestDTO dispatchData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to dispatch trips", null));
    }

    try {
      Trip dispatchedTrip = tripServiceImp.dispatchTrip(tenantId, tokenTenant, user, id, dispatchData);
      if (dispatchedTrip.getDriver() == null || dispatchedTrip.getDriver().getId() == null) {
        return ResponseEntity.status(400).body(new ApiResponse<>(false, "Driver must be assigned before dispatching the trip", null));
      }

      TripResponseDTO response = TripResponseMapper.toDTO(dispatchedTrip);
      String trackingToken = jwtTokenUtil.generateTripTrackingToken(dispatchedTrip.getId(), dispatchedTrip.getDriver().getId());

      TripDispatchResponseDTO dispatchResponse = new TripDispatchResponseDTO();
      dispatchResponse.setTrip(response);
      dispatchResponse.setTrackingToken(trackingToken);

      // Broadcast update to all subscribers on the general topic
      messagingTemplate.convertAndSend("/topic/trips", response);

      return ResponseEntity.ok(new ApiResponse<>(true, "Trip dispatched successfully!", dispatchResponse));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/arrived/{id}")
  public ResponseEntity<ApiResponse<?>> arrivedTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripArrivedRequestDTO arrivedData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to makrk it as arrived trip", null));
    }

    try {
      Trip arrivedTrip = tripServiceImp.arrivedTrip(tenantId, tokenTenant, user, id, arrivedData);
      TripResponseDTO response = TripResponseMapper.toDTO(arrivedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip marked as arrived successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/start/{id}")
  public ResponseEntity<ApiResponse<?>> startTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripStartRequestDTO startData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to start trips", null));
    }

    try {
      Trip startedTrip = tripServiceImp.startTrip(tenantId, tokenTenant, user, id, startData);
      TripResponseDTO response = TripResponseMapper.toDTO(startedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip started successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasRole('ADMIN') or hasAuthority('CAN_DISPATCH_TRIP')")
  @PutMapping("/drop/{id}")
  public ResponseEntity<ApiResponse<?>> dropTrip(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request,
      @Valid @RequestBody TripDropRequestDTO dropData
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to mark as driver dropped guest", null));
    }

    try {
      Trip droppedTrip = tripServiceImp.dropTrip(tenantId, tokenTenant, user, id, dropData);
      TripResponseDTO response = TripResponseMapper.toDTO(droppedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip completed successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/execute-manual/{id}")
  public ResponseEntity<ApiResponse<TripResponseDTO>> executeManualTrip(
      @PathVariable @NotNull UUID id,
      @RequestBody ManualTripExecuteRequestDTO executeData,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UserAccount user = (UserAccount) request.getAttribute("user");

    try {
      Trip completedTrip = tripServiceImp.executeManualTrip(tenantId, tokenTenant, user, id, executeData);
      return ResponseEntity.ok(new ApiResponse<>(
          true,
          "Manual trip executed and completed successfully!",
          TripResponseMapper.toDTO(completedTrip)
      ));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasRole('ADMIN')")
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

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to allot trips", null));
    }
    try {
      Trip reassignedTrip = tripServiceImp.allotDriverVehicle(tokenTenant, tokenTenantId, user, tripId, allotData);
      TripResponseDTO response = TripResponseMapper.toDTO(reassignedTrip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip reassigned successfully!", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/assign-partner/{tripId}")
  public ResponseEntity<ApiResponse<?>> assignTripToPartnerVendor(
      @PathVariable @NotNull UUID tripId,
      @Valid @RequestBody TripPartnerVendorAssignRequestDTO payload,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID tokenTenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to assign-partner to trips", null));
    }

    try {
      Trip trip = tripServiceImp.assignTripToPartnerVendor(tokenTenant, tokenTenantId, tripId, payload, updatedBy);
      TripResponseDTO response = TripResponseMapper.toDTO(trip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip assigned to partner vendor successfully", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/assign-vendor/{tripId}")
  public ResponseEntity<ApiResponse<?>> assignVendorToTrip(
      @PathVariable @NotNull UUID tripId,
      @Valid @RequestBody TripOrganisationVendorAssignRequestDTO payload,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID tokenTenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to assign-vendor to trips", null));
    }

    try {
      Trip trip = tripServiceImp.assignVendorToTrip(tokenTenant, tokenTenantId, tripId, payload, updatedBy);
      TripResponseDTO response = TripResponseMapper.toDTO(trip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Vendor assigned to trip successfully", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }

  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/confirm-trip/{tripId}")
  public ResponseEntity<ApiResponse<?>> confirmTrip(
      @PathVariable @NotNull UUID tripId,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    UserAccount user = (UserAccount) request.getAttribute("user");

    String rolename = user.getRole().getName().toString();
    if (!rolename.equals("ADMIN") || rolename.equals("DRIVER") || rolename.equals("GUEST")) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, "You don't have permission to confirm trips", null));
    }
    try {
      Trip trip = tripServiceImp.confirmTrip(tokenTenant, tripId, updatedBy);
      TripResponseDTO response = TripResponseMapper.toDTO(trip);
      return ResponseEntity.ok(new ApiResponse<>(true, "Trip confirmed successfully", response));
    } catch (RuntimeException ex) {
      return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
    }
  }
}

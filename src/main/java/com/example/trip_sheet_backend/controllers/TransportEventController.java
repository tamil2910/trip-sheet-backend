package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.EventDtos.EventCreateRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventMutationResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.UpdateEventVehicleTypesRequest;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TransportEventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
@PreAuthorize("isAuthenticated()")
public class TransportEventController {
  private final TransportEventService eventService;

  public TransportEventController(TransportEventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<EventMutationResponse>> create(
      HttpServletRequest request, @Valid @RequestBody EventCreateRequest body) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    EventMutationResponse created = eventService.create(body, tenant, createdBy);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Event created successfully", created));
  }

  @PutMapping("/{id}")
  public ApiResponse<EventMutationResponse> update(HttpServletRequest request, @PathVariable UUID id,
      @Valid @RequestBody EventCreateRequest body) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");
    return new ApiResponse<>(true, "Event updated successfully", eventService.update(id, body, tenant, updatedBy));
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(HttpServletRequest request, Pageable pageable) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    Page<EventResponse> result = eventService.list(tenant, pageable);
    Map<String, Object> data = new HashMap<>();
    data.put("data", result.getContent());
    data.put("currentPage", result.getNumber());
    data.put("pageSize", result.getSize());
    data.put("currentPageCount", result.getNumberOfElements());
    data.put("totalItems", result.getTotalElements());
    data.put("totalPages", result.getTotalPages());
    data.put("isFirst", result.isFirst());
    data.put("isLast", result.isLast());
    data.put("hasNext", result.hasNext());
    data.put("hasPrevious", result.hasPrevious());
    return new ApiResponse<>(true, "Events fetched successfully", data);
  }

  @PutMapping("/{id}/vehicle-types")
  public ApiResponse<EventResponse> updateVehicleTypes(HttpServletRequest request, @PathVariable UUID id,
      @Valid @RequestBody UpdateEventVehicleTypesRequest body) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");
    return new ApiResponse<>(true, "Event vehicle types updated successfully",
        eventService.updateVehicleTypes(id, body.getVehicleTypeIds(), tenant, updatedBy));
  }

  @GetMapping("/{id}")
  public ApiResponse<EventResponse> get(HttpServletRequest request, @PathVariable UUID id) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    return new ApiResponse<>(true, "Event fetched successfully", eventService.get(id, tenant));
  }

  @GetMapping("/vehicle-types")
  public ApiResponse<List<EventResponse.VehicleTypeOption>> vehicleTypes() {
    return new ApiResponse<>(true, "Vehicle types fetched successfully", eventService.vehicleTypeOptions());
  }
}



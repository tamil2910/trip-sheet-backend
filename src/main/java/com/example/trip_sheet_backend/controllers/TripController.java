package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.Map;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripService.TripServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/trips")
public class TripController extends BaseController<Trip, UUID> {

  private final TripServiceImp tripServiceImp;

  public TripController(TripServiceImp tripServiceImp) {
    super(tripServiceImp);
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

  @Override
  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping("/{id}")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ApiResponse<Trip> getById(
      @PathVariable @NotNull UUID id,
      HttpServletRequest request
  ) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

    Trip trip = tripServiceImp.findByIdResource(tenantId, id);
    if (trip == null) {
      return new ApiResponse<>(false, "Trip not found", null);
    }

    TripResponseDTO response = TripResponseMapper.toDTO(trip);
    return (ApiResponse<Trip>) (ApiResponse) new ApiResponse<>(true, "Trip fetched successfully!", response);
  }

    @Override
  @PreAuthorize("hasAuthority('CAN_READ_TRIP')")
  @GetMapping
  public ApiResponse<Map<String, Object>> getAll(@RequestParam Map<String, Object> filters,
    Pageable pageable,
    HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");

      Page<Trip> result;
      if (filters != null && !filters.isEmpty()) {
        result = tripServiceImp.searchResources(tenantId, filters, pageable);
      } else {
        result = tripServiceImp.getAllResources(tenantId, pageable);
    }

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

      return new ApiResponse<>(true, "Trips fetched successfully!", response);
  }

}

package com.example.trip_sheet_backend.controllers;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.TripDtos.TripCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripResponseDTO;
import com.example.trip_sheet_backend.mappers.TripResponseMapper;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripStop;
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
    if (trip == null || Boolean.TRUE.equals(trip.getIsDeleted())) {
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

      Map<String, Object> effectiveFilters = new java.util.HashMap<>();
      if (filters != null) {
        effectiveFilters.putAll(filters);
      }
      effectiveFilters.put("isDeleted", false);

      Page<Trip> result;
      if (!effectiveFilters.isEmpty()) {
        result = tripServiceImp.searchResources(tenantId, effectiveFilters, pageable);
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

  @Override
  @PreAuthorize("hasAuthority('CAN_UPDATE_TRIP')")
  @PutMapping("/{id}")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ApiResponse<Trip> update(@PathVariable @NotNull UUID id, @Valid @RequestBody Trip payload, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");

    Trip existingTrip = tripServiceImp.findByIdResource(tenantId, id);
    if (existingTrip == null || Boolean.TRUE.equals(existingTrip.getIsDeleted())) {
      return new ApiResponse<>(false, "Trip not found", null);
    }

    applyUpdate(existingTrip, payload);
    if (updatedBy != null) {
      existingTrip.setUpdatedBy(updatedBy.toString());
    }

    Trip updatedTrip = tripServiceImp.updateResource(tenantId, id, existingTrip);

    if (updatedTrip == null) {
      return new ApiResponse<>(false, "Trip updation failed", null);
    }

    TripResponseDTO response = TripResponseMapper.toDTO(updatedTrip);
    return (ApiResponse<Trip>) (ApiResponse) new ApiResponse<>(true, "Trip updated successfully!", response);
  }

  @Override
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

  private void applyUpdate(Trip target, Trip source) {
    if (source.getTripCode() != null) target.setTripCode(source.getTripCode());
    if (source.getTripStatus() != null) target.setTripStatus(source.getTripStatus());
    if (source.getTripType() != null) target.setTripType(source.getTripType());
    if (source.getParentTrip() != null) target.setParentTrip(source.getParentTrip());

    if (source.getVendor() != null) target.setVendor(source.getVendor());
    if (source.getAssignedByVendor() != null) target.setAssignedByVendor(source.getAssignedByVendor());
    if (source.getPreviousVendor() != null) target.setPreviousVendor(source.getPreviousVendor());
    if (source.getOrganisation() != null) target.setOrganisation(source.getOrganisation());

    if (source.getNotes() != null) target.setNotes(source.getNotes());
    if (source.getDriver() != null) target.setDriver(source.getDriver());
    if (source.getVehicle() != null) target.setVehicle(source.getVehicle());
    if (source.getDutyType() != null) target.setDutyType(source.getDutyType());
    if (source.getVehicleType() != null) target.setVehicleType(source.getVehicleType());

    if (source.getStops() != null) {
      target.getStops().clear();
      for (TripStop stop : source.getStops()) {
        stop.setTrip(target);
        target.getStops().add(stop);
      }
    }
    if (source.getPassengers() != null) target.setPassengers(source.getPassengers());
    if (source.getBooker() != null) target.setBooker(source.getBooker());

    if (source.getPickupTime() != null) target.setPickupTime(source.getPickupTime());
    if (source.getStartDate() != null) target.setStartDate(source.getStartDate());
    if (source.getEndDate() != null) target.setEndDate(source.getEndDate());
    if (source.getStartOtp() != null) target.setStartOtp(source.getStartOtp());
    if (source.getEndOtp() != null) target.setEndOtp(source.getEndOtp());
  }

}

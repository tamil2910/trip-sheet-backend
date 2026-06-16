package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationResponseDTO;
import com.example.trip_sheet_backend.dtos.TripBillingAllocationDtos.TripBillingAllocationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripBillingAllocationService.TripBillingAllocationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/trip-billing-allocations")
public class TripBillingAllocationController {

  private final TripBillingAllocationService tripBillingAllocationService;

  public TripBillingAllocationController(TripBillingAllocationService tripBillingAllocationService) {
    this.tripBillingAllocationService = tripBillingAllocationService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<TripBillingAllocationResponseDTO>> createAllocation(
      @Valid @RequestBody TripBillingAllocationCreateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
        true,
        "Trip billing allocation created successfully",
        TripBillingAllocationResponseDTO.fromEntity(
            tripBillingAllocationService.createAllocation(body, tokenTenant, createdBy)
        )
    ));
  }

  @GetMapping("/trip/{tripId}")
  public ResponseEntity<ApiResponse<List<TripBillingAllocationResponseDTO>>> getAllocationsByTrip(
      @PathVariable UUID tripId,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<TripBillingAllocationResponseDTO> response = tripBillingAllocationService.getAllocationsByTrip(tripId, tokenTenant)
        .stream()
        .map(TripBillingAllocationResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Trip billing allocations fetched successfully", response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TripBillingAllocationResponseDTO>> getAllocationById(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Trip billing allocation fetched successfully",
        TripBillingAllocationResponseDTO.fromEntity(tripBillingAllocationService.getAllocationById(id, tokenTenant))
    ));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TripBillingAllocationResponseDTO>> updateAllocation(
      @PathVariable UUID id,
      @Valid @RequestBody TripBillingAllocationUpdateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Trip billing allocation updated successfully",
        TripBillingAllocationResponseDTO.fromEntity(
            tripBillingAllocationService.updateAllocation(id, body, tokenTenant, updatedBy)
        )
    ));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAllocation(@PathVariable UUID id, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID deletedBy = (UUID) request.getAttribute("updatedBy");

    tripBillingAllocationService.deleteAllocation(id, tokenTenant, deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip billing allocation deleted successfully", null));
  }
}

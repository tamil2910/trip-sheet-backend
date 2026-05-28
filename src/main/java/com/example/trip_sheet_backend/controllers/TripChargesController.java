package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargeResponseDto;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargesBulkCreateRequestDto;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.TripChargesUpdateRequestDto;
import com.example.trip_sheet_backend.models.TripCharges;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.services.TripChargesService.TripChargeServiceImp;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.trip_sheet_backend.response_setups.ApiResponse;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping("/trip-charges")
public class TripChargesController extends BaseController<TripCharges, UUID> {
  private final TripChargeServiceImp tripChargeService;
  private final ObjectMapper objectMapper;

  public TripChargesController(TripChargeServiceImp tripChargeService, ObjectMapper objectMapper) {
    super(tripChargeService);
    this.tripChargeService = tripChargeService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<TripChargeResponseDto>> createTripCharge(@RequestBody TripChargesBulkCreateRequestDto payload, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID userId = (UUID) request.getAttribute("userId");
    UserAccount user = request.getAttribute("user") != null ? (UserAccount) request.getAttribute("user") : null;

    if(user == null) {
      throw new RuntimeException("User not found in token");
    }

    UUID tripId = UUID.fromString(payload.getTripId());

    if (tripId == null) {
      throw new RuntimeException("Trip id is required");
    }

    TripCharges createdTripCharge = tripChargeService.createTripCharges(tenantId, payload, user);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip charges created successfully", TripChargeResponseDto.fromEntity(createdTripCharge)));
  }

  @PutMapping("update/{id}")
  public ResponseEntity<ApiResponse<TripChargeResponseDto>> updateTripCharge(@PathVariable UUID id, @RequestBody TripChargesUpdateRequestDto payload, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    UUID userId = (UUID) request.getAttribute("userId");

    TripCharges tripCharges = objectMapper.convertValue(payload, TripCharges.class);
    tripCharges.setUpdatedBy(userId.toString());

    TripCharges updatedTripCharge = tripChargeService.updateResource(tenantId, id, tripCharges);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip charge updated successfully", TripChargeResponseDto.fromEntity(updatedTripCharge)));
  }

  @GetMapping("/trip/{tripId}")
  public ResponseEntity<ApiResponse<List<TripChargeResponseDto>>> getChargesOfTrip(@RequestParam UUID tripId, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    List<TripCharges> tripCharges = tripChargeService.getTripChargesByTripId(tenantId, tripId);
    List<TripChargeResponseDto> response = tripCharges.stream()
        .map(TripChargeResponseDto::fromEntity)
        .toList();
    return ResponseEntity.ok(new ApiResponse<>(true, "TripCharges fetched successfully", response));
  }

  @DeleteMapping("/{tripChargeId}")
  public ResponseEntity<ApiResponse<Void>> deleteTripCharge(@RequestParam UUID tripChargeId, HttpServletRequest request) {
    UUID tenantId = (UUID) request.getAttribute("tenantId");
    tripChargeService.deleteResource(tenantId, tripChargeId);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip charge deleted successfully", null));
  }
  
}

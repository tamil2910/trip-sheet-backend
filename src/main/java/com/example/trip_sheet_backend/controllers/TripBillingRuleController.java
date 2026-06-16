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

import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleResponseDTO;
import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripBillingRuleService.TripBillingRuleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/trip-billing-rules")
public class TripBillingRuleController {

  private final TripBillingRuleService tripBillingRuleService;

  public TripBillingRuleController(TripBillingRuleService tripBillingRuleService) {
    this.tripBillingRuleService = tripBillingRuleService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<TripBillingRuleResponseDTO>> createRule(
      @Valid @RequestBody TripBillingRuleCreateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
        true,
        "Trip billing rule created successfully",
        TripBillingRuleResponseDTO.fromEntity(tripBillingRuleService.createRule(body, tokenTenant, createdBy))
    ));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<TripBillingRuleResponseDTO>>> getRules(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<TripBillingRuleResponseDTO> response = tripBillingRuleService.getRulesByTenant(tokenTenant)
        .stream()
        .map(TripBillingRuleResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Trip billing rules fetched successfully", response));
  }

  @GetMapping("/active")
  public ResponseEntity<ApiResponse<TripBillingRuleResponseDTO>> getActiveRule(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Active trip billing rule fetched successfully",
        tripBillingRuleService.getActiveRule(tokenTenant) == null ? null
            : TripBillingRuleResponseDTO.fromEntity(tripBillingRuleService.getActiveRule(tokenTenant))
    ));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TripBillingRuleResponseDTO>> getRuleById(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Trip billing rule fetched successfully",
        TripBillingRuleResponseDTO.fromEntity(tripBillingRuleService.getRuleById(id, tokenTenant))
    ));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TripBillingRuleResponseDTO>> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody TripBillingRuleUpdateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Trip billing rule updated successfully",
        TripBillingRuleResponseDTO.fromEntity(tripBillingRuleService.updateRule(id, body, tokenTenant, updatedBy))
    ));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID deletedBy = (UUID) request.getAttribute("updatedBy");

    tripBillingRuleService.deleteRule(id, tokenTenant, deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Trip billing rule deleted successfully", null));
  }
}

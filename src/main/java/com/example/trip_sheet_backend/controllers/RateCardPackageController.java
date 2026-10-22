package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.trip_sheet_backend.dtos.RateCardPackageDtos.RateCardPackageCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.RateCardPackageDtos.RateCardPackageResponseDTO;
import com.example.trip_sheet_backend.dtos.RateCardPackageDtos.RateCardPackageUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.ratecardpackage.RateCardPackage;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.RateCardPackageService.RateCardPackageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/rate-card-packages")
public class RateCardPackageController {
  private final RateCardPackageService service;

  public RateCardPackageController(RateCardPackageService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<RateCardPackageResponseDTO>> create(
      @Valid @RequestBody RateCardPackageCreateRequestDTO body, HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID actorId = (UUID) request.getAttribute("createdBy");
    RateCardPackage created = service.create(body, tenant, actorId);
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
        "Rate card package created successfully", RateCardPackageResponseDTO.fromEntity(created)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<RateCardPackageResponseDTO>>> list(
      @RequestParam(required = false) UUID vendorPartnerId,
      @RequestParam(required = false) UUID vendorOrganisationId,
      HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    List<RateCardPackageResponseDTO> data = service.list(vendorPartnerId, vendorOrganisationId, tenant)
        .stream().map(RateCardPackageResponseDTO::fromEntity).toList();
    return ResponseEntity.ok(new ApiResponse<>(true, "Rate card packages fetched successfully", data));
  }

  @PutMapping("/{packageId}/default")
  public ResponseEntity<ApiResponse<RateCardPackageResponseDTO>> setDefault(
      @PathVariable UUID packageId, HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID actorId = (UUID) request.getAttribute("createdBy");
    RateCardPackage selected = service.setDefault(packageId, tenant, actorId);
    return ResponseEntity.ok(new ApiResponse<>(true, "Default rate card package updated successfully",
        RateCardPackageResponseDTO.fromEntity(selected)));
  }

  @PutMapping("/{packageId}")
  public ResponseEntity<ApiResponse<RateCardPackageResponseDTO>> update(
      @PathVariable UUID packageId, @Valid @RequestBody RateCardPackageUpdateRequestDTO body,
      HttpServletRequest request) {
    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID actorId = (UUID) request.getAttribute("createdBy");
    RateCardPackage updated = service.update(packageId, body, tenant, actorId);
    return ResponseEntity.ok(new ApiResponse<>(true, "Rate card package updated successfully",
        RateCardPackageResponseDTO.fromEntity(updated)));
  }
}

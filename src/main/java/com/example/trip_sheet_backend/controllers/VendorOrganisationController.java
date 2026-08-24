package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorOrganisationDtos.VendorOrganisationResponseDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationDtos.VendorOrganisationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorOrganisationService.VendorOrganisationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-organisations")
public class VendorOrganisationController {
  private final VendorOrganisationService vendorOrganisationService;

  public VendorOrganisationController(VendorOrganisationService vendorOrganisationService) {
    this.vendorOrganisationService = vendorOrganisationService;
  }

  @PutMapping("/{vendorOrganisationId}")
  public ResponseEntity<ApiResponse<VendorOrganisationResponseDTO>> update(
      @PathVariable UUID vendorOrganisationId,
      @Valid @RequestBody VendorOrganisationUpdateRequestDTO body,
      HttpServletRequest request) {
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");
    VendorOrganisation updated = vendorOrganisationService.update(
        vendorOrganisationId, body, loggedInTenant, updatedBy);

    return ResponseEntity.ok(new ApiResponse<>(true,
        "Vendor organisation updated successfully", VendorOrganisationResponseDTO.fromEntity(updated)));
  }
}

package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.VendorPartnerDtos.VendorPartnerResponseDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerDtos.VendorPartnerUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorPartnerService.VendorPartnerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-partners")
public class VendorPartnerController {
  private final VendorPartnerService vendorPartnerService;

  public VendorPartnerController(VendorPartnerService vendorPartnerService) {
    this.vendorPartnerService = vendorPartnerService;
  }

  @PutMapping("/{vendorPartnerId}")
  public ResponseEntity<ApiResponse<VendorPartnerResponseDTO>> update(
      @PathVariable UUID vendorPartnerId,
      @Valid @RequestBody VendorPartnerUpdateRequestDTO body,
      HttpServletRequest request) {
    Tenant loggedInTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("createdBy");
    VendorPartner updated = vendorPartnerService.update(vendorPartnerId, body, loggedInTenant, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true,
        "Vendor partner updated successfully", VendorPartnerResponseDTO.fromEntity(updated)));
  }
}

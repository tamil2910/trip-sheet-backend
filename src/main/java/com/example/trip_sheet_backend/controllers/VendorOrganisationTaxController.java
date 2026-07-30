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

import com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos.VendorOrganisationTaxCreateRequestDto;
import com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos.VendorOrganisationTaxResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VendorOrganisationTaxService.VendorOrganisationTaxService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendor-organisation-taxes")
public class VendorOrganisationTaxController {

  private final VendorOrganisationTaxService vendorOrganisationTaxService;

  public VendorOrganisationTaxController(VendorOrganisationTaxService vendorOrganisationTaxService) {
    this.vendorOrganisationTaxService = vendorOrganisationTaxService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<VendorOrganisationTaxResponseDto>> createVendorOrganisationTax(
      @Valid @RequestBody VendorOrganisationTaxCreateRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    VendorOrganisationTax created = vendorOrganisationTaxService.createVendorOrganisationTax(body, tokenTenant, createdBy);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Vendor organisation tax created successfully", VendorOrganisationTaxResponseDto.fromEntity(created)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<VendorOrganisationTaxResponseDto>>> getVendorOrganisationTaxes(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<VendorOrganisationTaxResponseDto> response = vendorOrganisationTaxService.getVendorOrganisationTaxes(tokenTenant)
        .stream()
        .map(VendorOrganisationTaxResponseDto::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Vendor organisation taxes fetched successfully", response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<VendorOrganisationTaxResponseDto>> getVendorOrganisationTaxById(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    VendorOrganisationTax item = vendorOrganisationTaxService.getVendorOrganisationTaxById(id, tokenTenant);

    return ResponseEntity.ok(new ApiResponse<>(true, "Vendor organisation tax fetched successfully", VendorOrganisationTaxResponseDto.fromEntity(item)));
  }

  @GetMapping("/vendor-organisation/{vendorOrganisationId}")
  public ResponseEntity<ApiResponse<List<VendorOrganisationTaxResponseDto>>> getVendorOrganisationTaxesByVendorOrganisation(
      @PathVariable UUID vendorOrganisationId,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<VendorOrganisationTaxResponseDto> response = vendorOrganisationTaxService
        .getVendorOrganisationTaxesByVendorOrganisation(vendorOrganisationId, tokenTenant)
        .stream()
        .map(VendorOrganisationTaxResponseDto::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Vendor organisation taxes fetched successfully", response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<VendorOrganisationTaxResponseDto>> updateVendorOrganisationTax(
      @PathVariable UUID id,
      @Valid @RequestBody VendorOrganisationTaxCreateRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    if (updatedBy == null) {
      updatedBy = (UUID) request.getAttribute("createdBy");
    }

    VendorOrganisationTax updated = vendorOrganisationTaxService.updateVendorOrganisationTax(id, body, tokenTenant, updatedBy);

    return ResponseEntity.ok(new ApiResponse<>(true, "Vendor organisation tax updated successfully", VendorOrganisationTaxResponseDto.fromEntity(updated)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteVendorOrganisationTax(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID deletedBy = (UUID) request.getAttribute("updatedBy");
    if (deletedBy == null) {
      deletedBy = (UUID) request.getAttribute("createdBy");
    }

    vendorOrganisationTaxService.deleteVendorOrganisationTax(id, tokenTenant, deletedBy);

    return ResponseEntity.ok(new ApiResponse<>(true, "Vendor organisation tax deleted successfully", null));
  }
}

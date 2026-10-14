package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.CustomFieldDtos.CreateCustomFieldRequestDto;
import com.example.trip_sheet_backend.dtos.CustomFieldDtos.UpdateCustomFieldRequestDto;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.CustomFieldService.CustomFieldService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom-fields")
public class CustomFieldController {

  private final CustomFieldService customFieldService;

  public CustomFieldController(CustomFieldService customFieldService) {
    this.customFieldService = customFieldService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<CustomField>> createCustomField(
      @Valid @RequestBody CreateCustomFieldRequestDto body,
      HttpServletRequest request) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    CustomField created = customFieldService.createCustomField(
        body.getName(),
      body.getOrganisationId(),
        body.getOwnerVendorPartnerId(),
        tenant,
        createdBy);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(true, "Custom field created successfully", created));
  }

  @GetMapping("/vendor/list")
  public ResponseEntity<ApiResponse<List<CustomField>>> listOrganisationFieldsForVendor(
      @RequestParam(value = "organisationId", required = false) UUID organisationId,
      @RequestParam(value = "ownerVendorPartnerId", required = false) UUID ownerVendorPartnerId,
      HttpServletRequest request) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    if (tenant.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenant can use this endpoint");
    }

    if ((organisationId == null) == (ownerVendorPartnerId == null)) {
      throw new RuntimeException("Provide exactly one of organisationId or ownerVendorPartnerId");
    }
    List<CustomField> fields = ownerVendorPartnerId != null
        ? customFieldService.getByAssociateCustomerForVendor(tenant, ownerVendorPartnerId)
        : customFieldService.getByOrganisationForVendor(tenant, organisationId);

    return ResponseEntity.ok(new ApiResponse<>(true, "Custom fields fetched successfully", fields));
  }

  @GetMapping("/my/list")
  public ResponseEntity<ApiResponse<List<CustomField>>> listMyCustomFields(HttpServletRequest request) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    if (tenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
      throw new RuntimeException("Only organisation tenant can view own custom fields");
    }

    List<CustomField> fields = customFieldService.getByOrganisation(tenant);

    return ResponseEntity.ok(new ApiResponse<>(true, "Custom fields fetched successfully", fields));
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<ApiResponse<CustomField>> updateCustomField(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCustomFieldRequestDto body,
      HttpServletRequest request) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    CustomField updated;
    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      updated = customFieldService.updateForOrganisation(id, body.getName(), tenant, updatedBy);
    } else if (tenant.getTenantType() == Tenant.TenantType.VENDOR) {
      updated = customFieldService.updateForVendor(id, body.getName(), tenant, updatedBy);
    } else {
      throw new RuntimeException("Only organisation or vendor tenant can update custom fields");
    }

    return ResponseEntity.ok(new ApiResponse<>(true, "Custom field updated successfully", updated));
  }
}

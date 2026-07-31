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

import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsResponseDTO;
import com.example.trip_sheet_backend.dtos.OrganisationSettingsDtos.OrganisationSettingsUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.OrganisationSettingsService.OrganisationSettingsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/organisation-settings")
public class OrganisationSettingsController {

  private final OrganisationSettingsService organisationSettingsService;

  public OrganisationSettingsController(OrganisationSettingsService organisationSettingsService) {
    this.organisationSettingsService = organisationSettingsService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<OrganisationSettingsResponseDTO>> createSettings(
      @Valid @RequestBody OrganisationSettingsCreateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");

    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
        true,
        "Organisation settings created successfully",
        OrganisationSettingsResponseDTO.fromEntity(organisationSettingsService.createSettings(body, tokenTenant, createdBy))
    ));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<OrganisationSettingsResponseDTO>>> getSettings(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    List<OrganisationSettingsResponseDTO> response = organisationSettingsService.getSettingsByTenant(tokenTenant)
        .stream()
        .map(OrganisationSettingsResponseDTO::fromEntity)
        .toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Organisation settings fetched successfully", response));
  }

  @GetMapping("/current")
  public ResponseEntity<ApiResponse<OrganisationSettingsResponseDTO>> getCurrentSettings(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Current organisation settings fetched successfully",
        organisationSettingsService.getCurrentSettings(tokenTenant) == null ? null
            : OrganisationSettingsResponseDTO.fromEntity(organisationSettingsService.getCurrentSettings(tokenTenant))
    ));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<OrganisationSettingsResponseDTO>> getSettingsById(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Organisation settings fetched successfully",
        OrganisationSettingsResponseDTO.fromEntity(organisationSettingsService.getSettingsById(id, tokenTenant))
    ));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<OrganisationSettingsResponseDTO>> updateSettings(
      @PathVariable UUID id,
      @Valid @RequestBody OrganisationSettingsUpdateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");

    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Organisation settings updated successfully",
        OrganisationSettingsResponseDTO.fromEntity(organisationSettingsService.updateSettings(id, body, tokenTenant, updatedBy))
    ));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSettings(@PathVariable UUID id, HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID deletedBy = (UUID) request.getAttribute("updatedBy");

    organisationSettingsService.deleteSettings(id, tokenTenant, deletedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Organisation settings deleted successfully", null));
  }
}

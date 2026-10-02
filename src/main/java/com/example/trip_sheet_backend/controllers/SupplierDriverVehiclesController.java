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
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierDriverVehiclesCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.SupplierDriverVehiclesDtos.SupplierDriverVehiclesResponseDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.SupplierDriverVehiclesService.SupplierDriverVehiclesService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/supplier-driver-vehicles")
public class SupplierDriverVehiclesController {
  private final SupplierDriverVehiclesService service;

  public SupplierDriverVehiclesController(SupplierDriverVehiclesService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SupplierDriverVehiclesResponseDTO>> create(
      @Valid @RequestBody SupplierDriverVehiclesCreateRequestDTO body,
      HttpServletRequest request
  ) {
    Tenant primaryVendor = (Tenant) request.getAttribute("tenant");
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    SupplierDriverVehicles created = service.create(body, primaryVendor,
        createdBy == null ? null : createdBy.toString());
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
        true,
        "Partner driver or vehicle created successfully",
        SupplierDriverVehiclesResponseDTO.fromEntity(created)
    ));
  }

  @GetMapping("/partner/{partnerVendorId}")
  public ResponseEntity<ApiResponse<List<SupplierDriverVehiclesResponseDTO>>> getByPartner(
      @PathVariable UUID partnerVendorId,
      HttpServletRequest request
  ) {
    Tenant primaryVendor = (Tenant) request.getAttribute("tenant");
    List<SupplierDriverVehiclesResponseDTO> entries = service.getByPartner(primaryVendor, partnerVendorId)
        .stream()
        .map(SupplierDriverVehiclesResponseDTO::fromEntity)
        .toList();
    return ResponseEntity.ok(new ApiResponse<>(true, "Partner drivers and vehicles fetched successfully", entries));
  }
}
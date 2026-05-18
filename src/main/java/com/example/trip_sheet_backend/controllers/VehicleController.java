package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleInfoDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleUpdateRequestDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Vehicle;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleService.VehicleServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/vehicles")
public class VehicleController extends GlobalBaseController<Vehicle, UUID> {
  private final VehicleServiceImp service;

  public VehicleController(VehicleServiceImp service) {
    super(service);
    this.service = service;
  }

  @PostMapping("/create")
  @PreAuthorize("hasAuthority('CAN_CREATE_VEHICLE')")
  public ResponseEntity<ApiResponse<VehicleCreateOrLinkResponseDto>> createVehicle(
      @Valid @RequestBody VehicleInfoDto body,
      HttpServletRequest request
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    VehicleCreateOrLinkResponseDto response = service.createOrLinkVehicle(body, tokenTenant, createdBy);
    String message = switch (response.getAction()) {
      case "VEHICLE_CREATED" -> "Vehicle created and linked successfully";
      case "VEHICLE_LINKED" -> "Existing vehicle linked successfully";
      default -> "Vehicle is already linked with this tenant";
    };

    return ResponseEntity.ok(new ApiResponse<>(true, message, response));
  }

  @PreAuthorize("hasAuthority('CAN_READ_VEHICLE')")
  @GetMapping
  public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicles(
      @RequestParam Map<String, Object> filters,
      Pageable pageable,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    Page<VehicleTenantResponseDto> vehicles = service.getVehiclesByTenant(tokenTenant, filters, pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("data", vehicles.getContent());
    response.put("currentPage", vehicles.getNumber());
    response.put("pageSize", vehicles.getSize());
    response.put("currentPageCount", vehicles.getNumberOfElements());
    response.put("totalItems", vehicles.getTotalElements());
    response.put("totalPages", vehicles.getTotalPages());
    response.put("isFirst", vehicles.isFirst());
    response.put("isLast", vehicles.isLast());
    response.put("hasNext", vehicles.hasNext());
    response.put("hasPrevious", vehicles.hasPrevious());

    return ResponseEntity.ok(new ApiResponse<>(true, "Vehicles fetched successfully", response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('CAN_READ_VEHICLE')")
  public ResponseEntity<ApiResponse<VehicleTenantResponseDto>> getVehicle(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    VehicleTenantResponseDto vehicle = service.getVehicleByTenant(tokenTenant, id);
    return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle fetched successfully", vehicle));
  }

  @GetMapping("/by-code/{uniqueCode}")
  @PreAuthorize("hasAuthority('CAN_READ_VEHICLE')")
  public ResponseEntity<ApiResponse<VehicleCodeLookupResponseDto>> getVehicleByUniqueCode(
      @PathVariable String uniqueCode,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    VehicleCodeLookupResponseDto vehicle = service.getVehicleByUniqueCode(tokenTenant, uniqueCode);
    return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle fetched successfully", vehicle));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLE')")
  public ResponseEntity<ApiResponse<VehicleTenantResponseDto>> updateVehicle(
      @PathVariable UUID id,
      @Valid @RequestBody VehicleUpdateRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    VehicleTenantResponseDto vehicle = service.updateVehicleByTenant(tokenTenant, id, body, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle updated successfully", vehicle));
  }
}

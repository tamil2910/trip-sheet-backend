package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCodeLookupResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverCreateOrLinkResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverTenantResponseDto;
import com.example.trip_sheet_backend.dtos.DriverDtos.DriverUpdateRequestDto;
import com.example.trip_sheet_backend.models.Driver;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.DriverService.DriverServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/drivers")
public class DriverController extends GlobalBaseController<Driver, UUID> {
  private final DriverServiceImp driverService;

  public DriverController(DriverServiceImp driverService) {
    super(driverService);
    this.driverService = driverService;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<DriverCreateOrLinkResponseDto>> createDriver(
      @Valid @RequestBody DriverCreateOrLinkRequestDto body,
      HttpServletRequest request
  ) {
    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    DriverCreateOrLinkResponseDto response = driverService.createOrLinkDriver(body, tokenTenant, createdBy);
    String message = switch (response.getAction()) {
      case "DRIVER_CREATED" -> "Driver created and linked successfully";
      case "DRIVER_LINKED" -> "Existing driver linked successfully";
      case "DRIVER_ALREADY_LINKED" -> "Driver is already linked with this tenant";
      default -> "Driver already exists. Use the returned unique code to link";
    };

    return ResponseEntity.ok(new ApiResponse<>(true, message, response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<DriverTenantResponseDto>>> getDrivers(HttpServletRequest request) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    List<DriverTenantResponseDto> drivers = driverService.getDriversByTenant(tokenTenant);
    return ResponseEntity.ok(new ApiResponse<>(true, "Drivers fetched successfully", drivers));
  }

  @GetMapping("/by-code/{uniqueCode}")
  public ResponseEntity<ApiResponse<DriverCodeLookupResponseDto>> getDriverByUniqueCode(
      @PathVariable String uniqueCode,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    DriverCodeLookupResponseDto driver = driverService.getDriverByUniqueCode(tokenTenant, uniqueCode);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver fetched successfully", driver));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> getDriver(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    DriverTenantResponseDto driver = driverService.getDriverByTenant(tokenTenant, id);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver fetched successfully", driver));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> updateDriver(
      @PathVariable UUID id,
      @Valid @RequestBody DriverUpdateRequestDto body,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.updateDriverByTenant(tokenTenant, id, body, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver updated successfully", driver));
  }

  @PatchMapping("/{id}/inactive")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> makeDriverInactiveForTenant(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.setDriverActiveForTenant(tokenTenant, id, false, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver marked inactive for current tenant", driver));
  }

  @PatchMapping("/{id}/active")
  public ResponseEntity<ApiResponse<DriverTenantResponseDto>> makeDriverActiveForTenant(
      @PathVariable UUID id,
      HttpServletRequest request
  ) {
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");
    UUID updatedBy = (UUID) request.getAttribute("updatedBy");
    DriverTenantResponseDto driver = driverService.setDriverActiveForTenant(tokenTenant, id, true, updatedBy);
    return ResponseEntity.ok(new ApiResponse<>(true, "Driver marked active for current tenant", driver));
  }
}

package com.example.trip_sheet_backend.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleDriverService.VehicleDriverServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/vehicle-driver-mapping")
public class VehicleDriverMappingController extends BaseController<VehicleDriverMapping, UUID>{
  private final VehicleDriverServiceImp service;
  public VehicleDriverMappingController(VehicleDriverServiceImp service) {
    super(service);
    this.service = service;
  }

  @PreAuthorize("hasAuthority('CAN_CREATE_VEHICLEDRIVERMAPPING')")
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<VehicleDriverMapping>> createVehicleAndDriverTogether(
          @Valid @RequestBody VehicleDriverCreateRequestDto body, HttpServletRequest request) {

      VehicleDriverMapping response = this.service.createVehicleAndDriver(body, request);

      return ResponseEntity.ok(
              new ApiResponse<>(true, "Vehicle & Driver Created Successfully!", response)
      );
  }

  @PreAuthorize("hasAuthority(@permissionResolver.readPermission(#root.this))")
  @GetMapping("/mapped")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getAllMappedVehiclesWithDriver(
      Pageable pageable,
      HttpServletRequest request
  ) {
      UUID tenantId = (UUID) request.getAttribute("tenantId");
      Map<String, Object> response = this.service.getAllMappedVehiclesWithDriver(tenantId, pageable);

      return ResponseEntity.ok(
          new ApiResponse<>(true, "Mapped vehicle-driver data fetched successfully", response)
      );
  }

}

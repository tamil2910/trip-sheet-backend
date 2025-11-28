package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleDriverService.VehicleDriverServiceImp;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/vehicle-driver-mapping")
public class VehicleDriverMappingController extends BaseController<VehicleDriverMapping, UUID>{
  private final VehicleDriverServiceImp service;
  public VehicleDriverMappingController(VehicleDriverServiceImp service) {
    super(service);
    this.service = service;
  }

  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  @PostMapping("/add")
  public ResponseEntity<ApiResponse<VehicleDriverMapping>> createVehicleAndDriverTogether(
          @Valid @RequestBody VehicleDriverCreateRequestDto body) {

      VehicleDriverMapping response = this.service.createVehicleAndDriver(body);

      return ResponseEntity.ok(
              new ApiResponse<>(true, "Vehicle & Driver Created Successfully!", response)
      );
  }

}

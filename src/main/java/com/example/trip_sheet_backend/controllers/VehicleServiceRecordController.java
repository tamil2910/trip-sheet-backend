package com.example.trip_sheet_backend.controllers;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.trip_sheet_backend.dtos.VehicleServiceRequest;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleServiceRecord;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.VehicleServiceRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController @RequestMapping("/vehicle-services")
public class VehicleServiceRecordController {
  private final VehicleServiceRecordService service;
  public VehicleServiceRecordController(VehicleServiceRecordService service) { this.service=service; }
  @GetMapping @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<List<VehicleServiceRecord>>> list(HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle services fetched successfully",service.list(tenantId(req)))); }
  @GetMapping("/{id}") @PreAuthorize("hasAuthority('CAN_READ_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> get(@PathVariable UUID id,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service fetched successfully",service.get(tenantId(req),id))); }
  @PostMapping @PreAuthorize("hasAuthority('CAN_CREATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> create(@Valid @RequestBody VehicleServiceRequest body,HttpServletRequest req) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,"Vehicle service created successfully",service.create(tenantId(req),body))); }
  @PutMapping("/{id}") @PreAuthorize("hasAuthority('CAN_UPDATE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<VehicleServiceRecord>> update(@PathVariable UUID id,@Valid @RequestBody VehicleServiceRequest body,HttpServletRequest req) { return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service updated successfully",service.update(tenantId(req),id,body))); }
  @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('CAN_DELETE_VEHICLESERVICE')")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id,HttpServletRequest req) { service.delete(tenantId(req),id); return ResponseEntity.ok(new ApiResponse<>(true,"Vehicle service deleted successfully",null)); }
  private static UUID tenantId(HttpServletRequest req) { return ((Tenant)req.getAttribute("tenant")).getId(); }
}

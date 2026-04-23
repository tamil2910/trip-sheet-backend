package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.Map;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

import org.springframework.data.domain.Pageable;

import jakarta.servlet.http.HttpServletRequest;

public interface VehicleDriverService {
  VehicleDriverMapping  createVehicleAndDriver(VehicleDriverCreateRequestDto dto, HttpServletRequest request);
  Map<String, Object> getAllMappedVehiclesWithDriver(UUID tenantId, Pageable pageable);
}

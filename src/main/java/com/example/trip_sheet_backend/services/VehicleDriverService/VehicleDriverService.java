package com.example.trip_sheet_backend.services.VehicleDriverService;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverLinkRequestDto;
import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverMappingResponseDto;

import jakarta.servlet.http.HttpServletRequest;

public interface VehicleDriverService {
  VehicleDriverMappingResponseDto createVehicleAndDriver(VehicleDriverCreateRequestDto dto, HttpServletRequest request);
  VehicleDriverMappingResponseDto linkDriverAndVehicle(VehicleDriverLinkRequestDto dto, HttpServletRequest request);
  VehicleDriverMappingResponseDto unlinkDriverAndVehicle(VehicleDriverLinkRequestDto dto, HttpServletRequest request);
  Map<String, Object> getAllMappedVehiclesWithDriver(UUID tenantId, Pageable pageable);
}

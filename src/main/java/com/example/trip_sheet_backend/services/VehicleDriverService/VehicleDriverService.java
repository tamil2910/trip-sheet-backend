package com.example.trip_sheet_backend.services.VehicleDriverService;


import com.example.trip_sheet_backend.dtos.DriverVehicleDtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

import jakarta.servlet.http.HttpServletRequest;

public interface VehicleDriverService {
  VehicleDriverMapping  createVehicleAndDriver(VehicleDriverCreateRequestDto dto, HttpServletRequest request);
}

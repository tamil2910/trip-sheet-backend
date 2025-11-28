package com.example.trip_sheet_backend.services.VehicleDriverService;

import com.example.trip_sheet_backend.dtos.VehicleDriverCreateRequestDto;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

public interface VehicleDriverService {
  VehicleDriverMapping  createVehicleAndDriver(VehicleDriverCreateRequestDto dto);
}

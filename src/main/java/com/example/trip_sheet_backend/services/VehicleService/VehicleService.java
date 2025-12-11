package com.example.trip_sheet_backend.services.VehicleService;

import java.util.UUID;

import com.example.trip_sheet_backend.common.services.GlobalBaseService;
import com.example.trip_sheet_backend.models.Vehicle;

public interface VehicleService extends GlobalBaseService<Vehicle, UUID> {
  Vehicle findByVehicleNumber(String vehicleNumber);
  Vehicle findByVehicleNumberAndTenantId(String vehicleNumber, UUID tenantId);
}

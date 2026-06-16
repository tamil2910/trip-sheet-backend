package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Vehicle;

public interface VehicleRepository extends BaseRepository<Vehicle, UUID> {
  Vehicle findByVehicleNumber(String vehicleNumber);
  Vehicle findByVehicleNumberAndTenant_Id(String vehicleNumber, UUID tenantId);
  Optional<Vehicle> findByVehicleUniqueCode(String vehicleUniqueCode);
  boolean existsByVehicleUniqueCode(String vehicleUniqueCode);

  Optional<Vehicle> findByTenant_IdAndVehicleNumber(UUID tenantId, String vehicleNumber);
  boolean existsByTenant_IdAndVehicleNumber(UUID tenantId, String vehicleNumber);

  Optional<Vehicle> findByTenant_IdAndModelName(UUID tenantId, String modelName);
  boolean existsByTenant_IdAndModelName(UUID tenantId, String modelName);

}

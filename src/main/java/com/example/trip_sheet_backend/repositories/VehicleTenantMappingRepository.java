package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VehicleTenantMapping;

@Repository
public interface VehicleTenantMappingRepository extends BaseRepository<VehicleTenantMapping, UUID> {

  boolean existsByVehicle_IdAndTenant_Id(UUID vehicleId, UUID tenantId);

  @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType", "tenant"})
  Optional<VehicleTenantMapping> findByVehicle_IdAndTenant_Id(UUID vehicleId, UUID tenantId);

  @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType", "tenant"})
  Optional<VehicleTenantMapping> findByTenant_IdAndVehicle_Id(UUID tenantId, UUID vehicleId);

  @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType", "tenant"})
  List<VehicleTenantMapping> findByTenant_Id(UUID tenantId);

  @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType", "tenant"})
  Optional<VehicleTenantMapping> findByTenant_IdAndVehicle_VehicleNumber(UUID tenantId, String vehicleNumber);

  @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType", "tenant"})
  Optional<VehicleTenantMapping> findByTenant_IdAndVehicle_ModelNameIgnoreCase(UUID tenantId, String modelName);
}

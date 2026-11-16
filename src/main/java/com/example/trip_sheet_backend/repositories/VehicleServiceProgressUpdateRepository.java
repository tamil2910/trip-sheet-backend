package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleServiceProgressUpdate;

public interface VehicleServiceProgressUpdateRepository extends JpaRepository<VehicleServiceProgressUpdate, UUID> {
  List<VehicleServiceProgressUpdate> findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(
      UUID vehicleServiceId, UUID tenantId);

  Optional<VehicleServiceProgressUpdate> findByIdAndVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalse(
      UUID id, UUID vehicleServiceId, UUID tenantId);
}

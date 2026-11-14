package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleServiceRecord;

public interface VehicleServiceRecordRepository extends JpaRepository<VehicleServiceRecord, UUID> {
  List<VehicleServiceRecord> findByTenant_IdAndIsDeletedFalseOrderByRequestDateDesc(UUID tenantId);
  Optional<VehicleServiceRecord> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

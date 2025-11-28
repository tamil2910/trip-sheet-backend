package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

@Repository
public interface VehicleDriverMappingRepository extends BaseRepository<VehicleDriverMapping, UUID> {
    Optional<VehicleDriverMapping> findByDriverIdAndTenantIdAndStatus(
      UUID driverId,
      UUID tenantId,
      VehicleDriverMapping.typeStatus status
    );
}

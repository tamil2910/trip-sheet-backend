package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.DriverTenantMapping;

@Repository
public interface DriverTenantMappingRepository extends BaseRepository<DriverTenantMapping, UUID> {

  boolean existsByDriver_IdAndTenant_Id(UUID driverId, UUID tenantId);

  Optional<DriverTenantMapping> findByDriver_IdAndTenant_Id(UUID driverId, UUID tenantId);
}

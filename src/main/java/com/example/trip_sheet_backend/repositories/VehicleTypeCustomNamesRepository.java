package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;


import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VehicleTypeCustomName;

public interface VehicleTypeCustomNamesRepository extends BaseRepository<VehicleTypeCustomName, UUID> {
  Optional<VehicleTypeCustomName> findByVehicleType_IdAndTenant_Id(UUID vehicleTypeId, UUID tenantId);
}

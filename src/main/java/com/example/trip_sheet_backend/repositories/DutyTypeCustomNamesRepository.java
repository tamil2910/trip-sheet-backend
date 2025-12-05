package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
// import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.DutyTypeCustomName;
// import com.example.trip_sheet_backend.models.Tenant;

public interface DutyTypeCustomNamesRepository extends BaseRepository<DutyTypeCustomName, UUID> {
  Optional<DutyTypeCustomName> findByDutyTypeIdAndTenantId(UUID dutyTypeId, UUID tenantId);
}

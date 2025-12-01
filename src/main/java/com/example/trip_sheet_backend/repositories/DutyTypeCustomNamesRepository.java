package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.DutyTypeCustomName;
// import com.example.trip_sheet_backend.models.Tenant;

public interface DutyTypeCustomNamesRepository extends JpaRepository<DutyTypeCustomName, UUID> {
  Optional<DutyTypeCustomName> findByDutyTypeIdAndTenantId(UUID dutyTypeId, UUID tenantId);
}

package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VehicleDriverMapping;

public interface VehicleDriverMappingRepository extends BaseRepository<VehicleDriverMapping, UUID> {
    Optional<VehicleDriverMapping> findByDriverIdAndTenantIdAndIsActive(
      UUID driverId,
      UUID tenantId,
      Boolean isActive
    );

    @Query(
      value = """
        SELECT vdm
        FROM VehicleDriverMapping vdm
        LEFT JOIN FETCH vdm.driver d
        LEFT JOIN FETCH d.account a
        LEFT JOIN FETCH vdm.vehicle v
        LEFT JOIN FETCH v.vehicleType vt
        WHERE vdm.tenant.id = :tenantId
          AND vdm.isDeleted = false
          AND vdm.isActive = true
        """,
      countQuery = """
        SELECT COUNT(vdm)
        FROM VehicleDriverMapping vdm
        WHERE vdm.tenant.id = :tenantId
          AND vdm.isDeleted = false
          AND vdm.isActive = true
        """
    )
    Page<VehicleDriverMapping> findAllMappedVehiclesWithDriverByTenantId(
      @Param("tenantId") UUID tenantId,
      Pageable pageable
    );

    Optional<VehicleDriverMapping> findByDriverIdAndVehicleIdAndTenantId(
      UUID driverId,
      UUID vehicleId,
      UUID tenantId
    );

    Optional<VehicleDriverMapping> findByVehicleIdAndTenantIdAndIsActive(
      UUID vehicleId,
      UUID tenantId,
      Boolean isActive
    );
}

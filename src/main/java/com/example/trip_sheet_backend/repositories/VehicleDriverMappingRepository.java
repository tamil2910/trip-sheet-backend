package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query("""
        UPDATE VehicleDriverMapping vdm
        SET vdm.updatedBy = :updatedBy
        WHERE vdm.driver.id = :driverId
          AND vdm.vehicle.id = :vehicleId
          AND vdm.tenant.id = :tenantId
    """)
    int updateDriverVehicleTenantMapping(
        @Param("driverId") UUID driverId,
        @Param("vehicleId") UUID vehicleId,
        @Param("updatedBy") String updatedBy,
        @Param("tenantId") UUID tenantId
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE VehicleDriverMapping vdm
        SET vdm.updatedBy = :updatedBy
        WHERE vdm.driver.id = :driverId
          AND vdm.vehicle.id = :vehicleId
          AND vdm.tenant.id = :tenantId
    """)
    int updateDriverVehicleTenantMappingInActive(
        @Param("driverId") UUID driverId,
        @Param("vehicleId") UUID vehicleId,
        @Param("updatedBy") String updatedBy,
        @Param("tenantId") UUID tenantId
    );

    Optional<VehicleDriverMapping> findByDriverIdAndVehicleIdNotAndTenantIdAndIsActiveTrue(UUID driverId, UUID vehicleId, UUID tenantId);
    Optional<VehicleDriverMapping> findByDriverIdAndVehicleIdAndTenantIdAndIsActiveFalse(UUID driverId, UUID vehicleId, UUID tenantId);

    boolean existsByDriverIdAndVehicleIdNotAndTenantIdAndIsActiveTrue(UUID driverId, UUID vehicleId, UUID tenantId);

    boolean existsByDriverIdAndVehicleIdAndTenantIdAndIsActiveTrue(UUID driverId, UUID vehicleId, UUID tenantId);

    Optional<VehicleDriverMapping> findByDriverIdNotAndVehicleIdAndTenantIdAndIsActiveTrue(UUID driverId, UUID vehicleId, UUID tenantId);

    boolean existsByDriverIdNotAndVehicleIdAndTenantIdAndIsActiveTrue(UUID driverId, UUID vehicleId, UUID tenantId);

     Optional<VehicleDriverMapping> findByDriverIdAndVehicleIdAndTenantIdAndIsActiveTrue(
      UUID driverId,
      UUID vehicleId,
      UUID tenantId
    );

    Optional<VehicleDriverMapping> findByDriverIdAndTenantIdAndIsActiveTrue(UUID driverId, UUID tenantId);
}

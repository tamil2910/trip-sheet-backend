package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.DriverTenantMapping;

public interface DriverTenantMappingRepository extends BaseRepository<DriverTenantMapping, UUID> {

  boolean existsByDriver_IdAndTenant_Id(UUID driverId, UUID tenantId);

  Optional<DriverTenantMapping> findByDriver_IdAndTenant_Id(UUID driverId, UUID tenantId);

  @EntityGraph(attributePaths = {"driver", "driver.account", "tenant"})
  List<DriverTenantMapping> findByTenant_Id(UUID tenantId);

  @EntityGraph(attributePaths = {"driver", "driver.account", "tenant"})
  Optional<DriverTenantMapping> findByTenant_IdAndDriver_Id(UUID tenantId, UUID driverId);

  @EntityGraph(attributePaths = {"driver", "driver.account", "tenant"})
  @Query("""
      SELECT mapping
      FROM DriverTenantMapping mapping
      JOIN mapping.driver driver
      LEFT JOIN driver.account account
      WHERE mapping.tenant.id = :tenantId
        AND (:fullName IS NULL OR LOWER(driver.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
        AND (:phone IS NULL OR LOWER(account.phone) LIKE LOWER(CONCAT('%', :phone, '%')))
        AND (:email IS NULL OR LOWER(account.email) LIKE LOWER(CONCAT('%', :email, '%')))
      """)
  Page<DriverTenantMapping> searchByTenantAndDriverFilters(
      @Param("tenantId") UUID tenantId,
      @Param("fullName") String fullName,
      @Param("phone") String phone,
      @Param("email") String email,
      Pageable pageable
  );

  List<DriverTenantMapping> findByDriver_Id(UUID driverId);

  @EntityGraph(attributePaths = {"tenant", "driver", "driver.account"})
  Optional<DriverTenantMapping> findByDriver_IdAndActiveTrue(UUID driverId);


}

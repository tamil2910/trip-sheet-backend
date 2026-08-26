package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;

import jakarta.persistence.LockModeType;

public interface TenantRepository extends BaseRepository<Tenant, UUID> {
  Optional<Tenant> findByTenantName(String tenantName);
  Optional<Tenant> findByGstNumber(String gstNumber);
  Optional<Tenant> findByContactEmail(String contactEmail);
  Optional<Tenant> findByAdmin_UserAccount_Id(UUID userAccountId);
  Optional<Tenant> findByTenantUniqueCodeIgnoreCase(String tenantUniqueCode);
  boolean existsByTenantUniqueCode(String tenantUniqueCode);

  /** Serializes first-time document-number configuration for one vendor. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select tenant from Tenant tenant where tenant.id = :id")
  Optional<Tenant> findByIdForUpdate(@Param("id") UUID id);
}

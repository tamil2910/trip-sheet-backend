package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PeopleTenant;

@Repository
public interface PeopleTenantRepository extends BaseRepository<PeopleTenant, UUID> {
    // -------- Organisation people --------
  Optional<PeopleTenant> findByPhoneAndOrganisation_Id(
      String phone,
      UUID organisationId
  );
    // -------- Organisation people --------
  Optional<PeopleTenant> findByNameAndPhoneAndOrganisation_Id(
    String name,
    String phone,
    UUID organisationId
  );

  // -------- Vendor-added people for organisation --------
  Optional<PeopleTenant> findByNameAndPhoneAndOrganisation_IdAndAttachedVendors_Id(
    String name,
    String phone,
    UUID organisationId,
    UUID vendorId
  );

  // -------- Walk-in people --------
  Optional<PeopleTenant> findByPhoneAndTenantType(
    String phone,
    PeopleTenant.PeopleTenantType tenantType
  );

  Page<PeopleTenant> findByOrganisation_Id(UUID organisationId, Pageable pageable);

  Page<PeopleTenant> findByOrganisation_IdAndAttachedVendors_Id(
      UUID organisationId,
      UUID vendorId,
      Pageable pageable
  );

  Page<PeopleTenant> findByTenantTypeAndAttachedVendors_Id(
      PeopleTenant.PeopleTenantType tenantType,
      UUID vendorId,
      Pageable pageable
  );

  Optional<PeopleTenant> findByEmail(String email);
  List<PeopleTenant> findAllByEmailOrderByCreatedAtDesc(String email);

}

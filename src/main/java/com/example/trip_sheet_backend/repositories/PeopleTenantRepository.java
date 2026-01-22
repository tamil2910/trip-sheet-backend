package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

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
}

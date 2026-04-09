package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;


public interface VendorOrganisationRepository extends BaseRepository<VendorOrganisation, UUID> {
  boolean existsByVendorAndOrganisation(
          Tenant vendor,
          Tenant organisation
  );

  List<VendorOrganisation> findByVendor(Tenant vendor);
  Page<VendorOrganisation> findByVendor(Tenant vendor, Pageable pageable);
  Page<VendorOrganisation> findByVendorAndOrganisation_TenantType(
      Tenant vendor,
      Tenant.TenantType tenantType,
      Pageable pageable
  );

  List<VendorOrganisation> findByOrganisation(Tenant organisation);
  Page<VendorOrganisation> findByOrganisation(Tenant organisation, Pageable pageable);

  Optional<VendorOrganisation> findByVendorAndOrganisation_Id(Tenant vendor, UUID organisationId);

  Optional<VendorOrganisation> findByOrganisationAndVendor_Id(Tenant organisation, UUID vendorId);
  
}

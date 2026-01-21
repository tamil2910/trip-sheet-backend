package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;


public interface VendorOrganisationRepository extends BaseRepository<VendorOrganisation, UUID> {
  boolean existsByVendorAndOrganisation(
          Tenant vendor,
          Tenant organisation
  );
}

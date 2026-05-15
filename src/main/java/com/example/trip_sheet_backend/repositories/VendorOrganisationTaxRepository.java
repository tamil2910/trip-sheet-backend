package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;

public interface VendorOrganisationTaxRepository extends BaseRepository<VendorOrganisationTax, UUID> {
  boolean existsByTenant_IdAndVendorOrganisation_IdAndTax_Id(UUID tenantId, UUID vendorOrganisationId, UUID taxId);
}

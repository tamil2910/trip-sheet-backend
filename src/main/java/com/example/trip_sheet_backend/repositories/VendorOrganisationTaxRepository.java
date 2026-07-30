package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;

public interface VendorOrganisationTaxRepository extends BaseRepository<VendorOrganisationTax, UUID> {
  boolean existsByTenant_IdAndVendorOrganisation_IdAndTax_Id(UUID tenantId, UUID vendorOrganisationId, UUID taxId);

  java.util.Optional<VendorOrganisationTax> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

  List<VendorOrganisationTax> findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID tenantId);

  List<VendorOrganisationTax> findByVendorOrganisation_IdAndIsDeletedFalse(UUID vendorOrganisationId);

  List<VendorOrganisationTax> findByVendorOrganisation_IdAndTenant_IdAndIsDeletedFalse(UUID vendorOrganisationId, UUID tenantId);
}

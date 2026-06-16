package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorPartnerTax;

public interface VendorPartnerTaxRepository extends BaseRepository<VendorPartnerTax, UUID> {
  boolean existsByTenant_IdAndVendorPartner_IdAndTax_Id(UUID tenantId, UUID vendorPartnerId, UUID taxId);

  List<VendorPartnerTax> findByVendorPartner_IdAndIsDeletedFalse(UUID vendorPartnerId);
}

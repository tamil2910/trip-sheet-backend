package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;

public interface VendorOrganisationRateCardRepository extends BaseRepository<VendorOrganisationRateCard, UUID> {
  List<VendorOrganisationRateCard> findByVendorOrganisationId(UUID vendorOrganisationId);
}

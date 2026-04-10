package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;

public interface VendorPartnerRateCardRepository extends BaseRepository<VendorPartnerRateCard, UUID> {
  List<VendorPartnerRateCard> findByVendorPartnerIdAndIsDeletedFalse(UUID vendorPartnerId);
}

package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;


public interface VendorPartnerRepository extends BaseRepository<VendorPartner, UUID> {
  boolean existsByPrimaryVendorAndPartnerVendor(
          Tenant primaryVendor,
          Tenant partnerVendor
  );
}

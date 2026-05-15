package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorPartner;


public interface VendorPartnerRepository extends BaseRepository<VendorPartner, UUID> {
  boolean existsByPrimaryVendorAndPartnerVendor(
          Tenant primaryVendor,
          Tenant partnerVendor
  );

  Optional<VendorPartner> findByPrimaryVendorAndPartnerVendor(Tenant primaryVendor, Tenant partnerVendor);

  List<VendorPartner> findByPrimaryVendor(Tenant primaryVendor);

  Page<VendorPartner> findByPrimaryVendor(Tenant primaryVendor, Pageable pageable);
  
}

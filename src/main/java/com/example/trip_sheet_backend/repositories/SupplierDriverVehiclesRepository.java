package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles;

public interface SupplierDriverVehiclesRepository
    extends BaseRepository<SupplierDriverVehicles, UUID> {

  List<SupplierDriverVehicles> findByPrimaryVendor_IdAndPartnerVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(
      UUID primaryVendorId,
      UUID partnerVendorId
  );

    boolean existsByPrimaryVendor_IdAndPartnerVendor_IdAndPhone(
      UUID primaryVendorId,
      UUID partnerVendorId,
      String phone
    );

    boolean existsByPrimaryVendor_IdAndPartnerVendor_IdAndEmailIgnoreCase(
      UUID primaryVendorId,
      UUID partnerVendorId,
      String email
    );

    boolean existsByPrimaryVendor_IdAndPartnerVendor_IdAndVehicleNumberIgnoreCase(
      UUID primaryVendorId,
      UUID partnerVendorId,
      String vehicleNumber
    );
}
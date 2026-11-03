package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles;
import com.example.trip_sheet_backend.models.SupplierDriverVehicles.Type;

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

  boolean existsByPrimaryVendor_IdAndPartnerVendor_IdAndDriver_IdAndTypeAndIsDeletedFalse(
      UUID primaryVendorId, UUID partnerVendorId, UUID driverId, Type type);

  Optional<SupplierDriverVehicles> findByIdAndPrimaryVendor_IdAndPartnerVendor_IdAndTypeAndIsDeletedFalse(
      UUID id, UUID primaryVendorId, UUID partnerVendorId, Type type);
}

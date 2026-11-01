package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorEmployeeAddress;
import com.example.trip_sheet_backend.models.VendorEmployeeAddress.AddressCategory;

public interface VendorEmployeeAddressRepository extends BaseRepository<VendorEmployeeAddress, UUID> {
    List<VendorEmployeeAddress> findAllByVendorEmployee_IdAndIsDeletedFalse(UUID vendorEmployeeId);
    Optional<VendorEmployeeAddress> findByIdAndVendorEmployee_IdAndIsDeletedFalse(UUID id, UUID vendorEmployeeId);
    boolean existsByVendorEmployee_IdAndCategoryAndIsDeletedFalse(UUID vendorEmployeeId, AddressCategory category);
    boolean existsByVendorEmployee_IdAndCategoryAndIdNotAndIsDeletedFalse(UUID vendorEmployeeId, AddressCategory category, UUID excludedId);
}

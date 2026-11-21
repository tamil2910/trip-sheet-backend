package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.VendorDelegationHistory;

public interface VendorDelegationHistoryRepository extends BaseRepository<VendorDelegationHistory, UUID> {
  List<VendorDelegationHistory> findByTrip_IdAndIsDeletedFalseOrderByDelegatedAtAscCreatedAtAsc(UUID tripId);

  List<VendorDelegationHistory> findByToVendor_IdAndResponseStatusAndIsDeletedFalseOrderByDelegatedAtAsc(
      UUID vendorId, VendorDelegationHistory.ResponseStatus responseStatus);

  java.util.Optional<VendorDelegationHistory> findFirstByTrip_IdAndToVendor_IdAndIsDeletedFalseOrderByDelegatedAtDesc(
      UUID tripId, UUID vendorId);
}

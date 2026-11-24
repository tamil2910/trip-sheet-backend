package com.example.trip_sheet_backend.repositories;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventVendorTransportRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventVendorTransportRequestRepository extends BaseRepository<EventVendorTransportRequest, UUID> {
  List<EventVendorTransportRequest> findAllByVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID vendorId);
  Optional<EventVendorTransportRequest> findByIdAndVendor_IdAndIsDeletedFalse(UUID id, UUID vendorId);
}

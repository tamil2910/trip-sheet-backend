package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PurchaseOrder;

public interface PurchaseOrderRepository extends BaseRepository<PurchaseOrder, UUID> {
  boolean existsByAllocation_IdAndIsDeletedFalse(UUID allocationId);

  List<PurchaseOrder> findByTripSummary_IdAndIsDeletedFalse(UUID tripSummaryId);
}

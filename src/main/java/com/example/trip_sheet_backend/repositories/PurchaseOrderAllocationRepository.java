package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PurchaseOrderAllocation;

public interface PurchaseOrderAllocationRepository extends BaseRepository<PurchaseOrderAllocation, UUID> {
  List<PurchaseOrderAllocation> findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtAsc(UUID purchaseOrderId);
}

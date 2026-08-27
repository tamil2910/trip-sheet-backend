package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Invoice;

public interface InvoiceRepository extends BaseRepository<Invoice, UUID> {
  List<Invoice> findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID purchaseOrderId);

  default Optional<Invoice> findLatestByPurchaseOrderId(UUID purchaseOrderId) {
    return findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtDesc(purchaseOrderId).stream().findFirst();
  }
}

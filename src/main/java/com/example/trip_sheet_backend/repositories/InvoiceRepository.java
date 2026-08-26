package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Invoice;

public interface InvoiceRepository extends BaseRepository<Invoice, UUID> {
  Optional<Invoice> findByPurchaseOrder_IdAndIsDeletedFalse(UUID purchaseOrderId);
}

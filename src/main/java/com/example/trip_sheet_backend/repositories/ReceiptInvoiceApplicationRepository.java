package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.ReceiptInvoiceApplication;

public interface ReceiptInvoiceApplicationRepository extends BaseRepository<ReceiptInvoiceApplication, UUID> {
    boolean existsByReceipt_IdAndIsDeletedFalse(UUID receiptId);
}

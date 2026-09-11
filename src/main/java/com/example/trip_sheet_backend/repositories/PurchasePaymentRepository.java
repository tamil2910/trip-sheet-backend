package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PurchasePayment;

public interface PurchasePaymentRepository extends BaseRepository<PurchasePayment, UUID> {
    @EntityGraph(attributePaths = { "payeeVendor", "fromBank", "purchaseInvoices" })
    List<PurchasePayment> findByPayerVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID payerVendorId);

    @EntityGraph(attributePaths = { "payeeVendor", "fromBank", "purchaseInvoices" })
    Optional<PurchasePayment> findByIdAndPayerVendor_IdAndIsDeletedFalse(UUID id, UUID payerVendorId);
}

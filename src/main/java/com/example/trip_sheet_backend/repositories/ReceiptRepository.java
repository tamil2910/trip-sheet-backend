package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Receipt;

public interface ReceiptRepository extends BaseRepository<Receipt, UUID> {
    @EntityGraph(attributePaths = { "organisation", "fromBank", "invoices" })
    List<Receipt> findByVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID vendorId);

    @EntityGraph(attributePaths = { "organisation", "fromBank", "invoices" })
    Optional<Receipt> findByIdAndVendor_IdAndIsDeletedFalse(UUID id, UUID vendorId);
}

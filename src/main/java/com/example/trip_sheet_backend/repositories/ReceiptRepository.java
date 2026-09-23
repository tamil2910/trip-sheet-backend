package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Receipt;

public interface ReceiptRepository extends BaseRepository<Receipt, UUID> {
    @Query(value = "select r.receipt_number from receipts r "
        + "where r.vendor_id = :vendorId and r.is_deleted = false "
        + "and r.receipt_number like concat('R-', :financialYear, '-%') "
        + "order by cast(substring_index(r.receipt_number, '-', -1) as unsigned) desc limit 1", nativeQuery = true)
    Optional<String> findLastReceiptNumber(@Param("vendorId") UUID vendorId,
        @Param("financialYear") String financialYear);

    @EntityGraph(attributePaths = { "organisation", "fromBank", "invoices" })
    List<Receipt> findByVendor_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID vendorId);

    @EntityGraph(attributePaths = { "organisation", "fromBank", "invoices" })
    Optional<Receipt> findByIdAndVendor_IdAndIsDeletedFalse(UUID id, UUID vendorId);
}

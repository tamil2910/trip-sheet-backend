package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PurchaseInvoice;

public interface PurchaseInvoiceRepository extends BaseRepository<PurchaseInvoice, UUID> {
  boolean existsByDelegationHistory_IdAndIsDeletedFalse(UUID delegationHistoryId);

  @Query("select p from PurchaseInvoice p where p.isDeleted = false "
      + "and (p.payerVendor.id = :tenantId or p.payeeVendor.id = :tenantId) order by p.createdAt desc")
  List<PurchaseInvoice> findVisibleToTenant(@Param("tenantId") UUID tenantId);

  Optional<PurchaseInvoice> findByIdAndIsDeletedFalse(UUID id);
}

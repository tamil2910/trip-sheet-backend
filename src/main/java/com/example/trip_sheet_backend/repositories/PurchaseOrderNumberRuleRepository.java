package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.PurchaseOrderNumberRule;

import jakarta.persistence.LockModeType;

public interface PurchaseOrderNumberRuleRepository extends BaseRepository<PurchaseOrderNumberRule, UUID> {
  long countByVendor_IdAndIsDeletedFalse(UUID vendorId);

  List<PurchaseOrderNumberRule> findByVendor_IdAndIsDeletedFalseOrderByUpdatedAtDesc(UUID vendorId);

  Optional<PurchaseOrderNumberRule> findByIdAndVendor_IdAndIsDeletedFalse(UUID id, UUID vendorId);

  Optional<PurchaseOrderNumberRule> findByVendor_IdAndIsDefaultTrueAndIsDeletedFalse(UUID vendorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PurchaseOrderNumberRule> findWithLockByVendor_IdAndIsDefaultTrueAndIsDeletedFalse(UUID vendorId);
}

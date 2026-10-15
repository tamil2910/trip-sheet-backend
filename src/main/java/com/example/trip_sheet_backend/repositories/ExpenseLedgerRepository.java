package com.example.trip_sheet_backend.repositories;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.ExpenseLedger;
public interface ExpenseLedgerRepository extends JpaRepository<ExpenseLedger, UUID> {
  List<ExpenseLedger> findByTenant_IdAndIsDeletedFalseOrderByName(UUID tenantId);
  List<ExpenseLedger> findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(UUID tenantId);
  Optional<ExpenseLedger> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

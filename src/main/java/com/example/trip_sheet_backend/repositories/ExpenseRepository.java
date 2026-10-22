package com.example.trip_sheet_backend.repositories;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.Expense;
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  List<Expense> findByTenant_IdAndIsDeletedFalseOrderByRequestedDateDesc(UUID tenantId);
  List<Expense> findByTenant_IdAndLedger_IdAndIsDeletedFalseOrderByRequestedDateDesc(UUID tenantId, UUID ledgerId);
  Optional<Expense> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

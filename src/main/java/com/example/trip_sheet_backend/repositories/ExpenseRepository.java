package com.example.trip_sheet_backend.repositories;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.example.trip_sheet_backend.models.Expense;
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  @EntityGraph(attributePaths = {"ledger", "ledger.category", "category", "subCategory", "bankAccount", "invoices"})
  List<Expense> findByTenant_IdAndIsDeletedFalseOrderByRequestedDateDesc(UUID tenantId);
  @EntityGraph(attributePaths = {"ledger", "ledger.category", "category", "subCategory", "bankAccount", "invoices"})
  List<Expense> findByTenant_IdAndLedger_IdAndIsDeletedFalseOrderByRequestedDateDesc(UUID tenantId, UUID ledgerId);
  @EntityGraph(attributePaths = {"ledger", "ledger.category", "category", "subCategory", "bankAccount", "invoices"})
  Optional<Expense> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

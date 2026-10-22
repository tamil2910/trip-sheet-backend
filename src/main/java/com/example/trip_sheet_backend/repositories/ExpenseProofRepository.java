package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.ExpenseProof;

public interface ExpenseProofRepository extends JpaRepository<ExpenseProof, UUID> {
  List<ExpenseProof> findByExpense_IdAndExpense_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(UUID expenseId, UUID tenantId);
}

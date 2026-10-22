package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.PettyCashBreakdown;

public interface PettyCashBreakdownRepository extends JpaRepository<PettyCashBreakdown, UUID> {
  List<PettyCashBreakdown> findByTenant_IdAndIsDeletedFalseOrderBySpentDateDesc(UUID tenantId);
  List<PettyCashBreakdown> findByTenant_IdAndExpenseIdAndIsDeletedFalseOrderBySpentDateDesc(UUID tenantId, UUID expenseId);
  Optional<PettyCashBreakdown> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

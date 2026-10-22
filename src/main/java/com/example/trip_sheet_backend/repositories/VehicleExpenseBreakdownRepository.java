package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleExpenseBreakdown;

public interface VehicleExpenseBreakdownRepository extends JpaRepository<VehicleExpenseBreakdown, UUID> {
  List<VehicleExpenseBreakdown> findByTenant_IdAndIsDeletedFalseOrderByExpenseDateDesc(UUID tenantId);
  List<VehicleExpenseBreakdown> findByTenant_IdAndExpenseIdAndIsDeletedFalseOrderByExpenseDateDesc(UUID tenantId, UUID expenseId);
  Optional<VehicleExpenseBreakdown> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

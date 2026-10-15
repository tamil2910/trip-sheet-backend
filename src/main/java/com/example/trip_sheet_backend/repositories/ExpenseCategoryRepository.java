package com.example.trip_sheet_backend.repositories;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.ExpenseCategory;
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
  List<ExpenseCategory> findByTenant_IdAndIsDeletedFalseOrderByName(UUID tenantId);
  List<ExpenseCategory> findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(UUID tenantId);
  Optional<ExpenseCategory> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

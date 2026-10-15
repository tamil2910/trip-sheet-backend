package com.example.trip_sheet_backend.repositories;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.ExpenseSubCategory;
public interface ExpenseSubCategoryRepository extends JpaRepository<ExpenseSubCategory, UUID> {
  List<ExpenseSubCategory> findByTenant_IdAndIsDeletedFalseOrderByName(UUID tenantId);
  List<ExpenseSubCategory> findByTenant_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(UUID tenantId);
  List<ExpenseSubCategory> findByTenant_IdAndCategory_IdAndIsDeletedFalseOrderByName(UUID tenantId, UUID categoryId);
  List<ExpenseSubCategory> findByTenant_IdAndCategory_IdAndIsDeletedFalseAndIsActiveTrueOrderByName(UUID tenantId, UUID categoryId);
  Optional<ExpenseSubCategory> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
}

package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleServiceItemCategory;

public interface VehicleServiceItemCategoryRepository extends JpaRepository<VehicleServiceItemCategory, UUID> {
  List<VehicleServiceItemCategory> findByTenant_IdAndIsDeletedFalseOrderByNameAsc(UUID tenantId);
  Optional<VehicleServiceItemCategory> findByTenant_IdAndNormalizedName(UUID tenantId, String normalizedName);
}

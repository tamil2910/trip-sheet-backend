package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleServiceExpenseItem;

public interface VehicleServiceExpenseItemRepository extends JpaRepository<VehicleServiceExpenseItem, UUID> {
  List<VehicleServiceExpenseItem> findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(
      UUID vehicleServiceId, UUID tenantId);
  void deleteByVehicleService_IdAndVehicleService_Tenant_Id(UUID vehicleServiceId, UUID tenantId);
}

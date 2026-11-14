package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.VehicleServiceInvoice;

public interface VehicleServiceInvoiceRepository extends JpaRepository<VehicleServiceInvoice, UUID> {
  List<VehicleServiceInvoice> findByVehicleService_IdAndVehicleService_Tenant_IdAndIsDeletedFalseOrderByCreatedAtAsc(
      UUID vehicleServiceId, UUID tenantId);
}

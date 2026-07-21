package com.example.trip_sheet_backend.services.PurchaseOrderService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tenant;

public interface PurchaseOrderService {
  List<PurchaseOrder> getPurchaseOrdersByTenant(Tenant tokenTenant);

  PurchaseOrder updatePurchaseOrder(UUID purchaseOrderId, PurchaseOrderUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy);

  void deletePurchaseOrder(UUID purchaseOrderId, Tenant tokenTenant, UUID deletedBy);
}

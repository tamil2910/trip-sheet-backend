package com.example.trip_sheet_backend.services.PurchaseOrderService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.CombinePurchaseOrdersRequestDTO;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tenant;

public interface PurchaseOrderService {
  Page<PurchaseOrder> getPurchaseOrdersByTenant(Tenant tokenTenant, Pageable pageable);

  PurchaseOrder updatePurchaseOrder(UUID purchaseOrderId, PurchaseOrderUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy);

  void deletePurchaseOrder(UUID purchaseOrderId, Tenant tokenTenant, UUID deletedBy);

  PurchaseOrder combinePurchaseOrders(CombinePurchaseOrdersRequestDTO body, Tenant vendor, UUID createdBy);

  void splitCombinedPurchaseOrder(UUID combinedPurchaseOrderId, Tenant vendor, UUID updatedBy);
}

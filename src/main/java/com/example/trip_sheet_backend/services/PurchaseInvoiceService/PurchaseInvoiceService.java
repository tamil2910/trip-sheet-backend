package com.example.trip_sheet_backend.services.PurchaseInvoiceService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Tenant;

public interface PurchaseInvoiceService {
  List<PurchaseInvoice> getForTenant(Tenant tenant);
  PurchaseInvoice getById(UUID id, Tenant tenant);
}

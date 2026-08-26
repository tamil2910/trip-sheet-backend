package com.example.trip_sheet_backend.services.InvoiceService;

import java.util.UUID;

import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.Tenant;

public interface InvoiceService {
  Invoice markPrinted(UUID invoiceId, Tenant tokenTenant, UUID actorId);

  Invoice markDownloaded(UUID invoiceId, Tenant tokenTenant, UUID actorId);

  Invoice updateStatus(UUID invoiceId, Invoice.InvoiceStatus status, Tenant tokenTenant, UUID actorId);
}

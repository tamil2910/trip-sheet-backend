package com.example.trip_sheet_backend.services.InvoiceService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.VendorInvoiceOutstandingResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.OrganisationVendorPayableResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.InvoiceOutstandingTotalsResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.OrganisationVendorPayableTotalsResponseDTO;

public interface InvoiceService {
  Page<Invoice> getInvoices(Tenant tokenTenant, Map<String, Object> filters, Pageable pageable);

  Invoice getByPurchaseOrderId(UUID purchaseOrderId, Tenant tokenTenant);

  Invoice markPrinted(UUID invoiceId, Tenant tokenTenant, UUID actorId);

  Invoice markDownloaded(UUID invoiceId, Tenant tokenTenant, UUID actorId);

  Invoice updateStatus(UUID invoiceId, Invoice.InvoiceStatus status, Tenant tokenTenant, UUID actorId);

  Invoice cancel(UUID invoiceId, Tenant tokenTenant, UUID actorId);

  VendorInvoiceOutstandingResponseDTO getVendorOutstanding(Tenant tokenTenant);

  VendorInvoiceOutstandingResponseDTO getVendorOutstandingForPeriod(Tenant tokenTenant, Long startDate, Long endDate,
      UUID organisationId);

  InvoiceOutstandingTotalsResponseDTO getVendorOutstandingTotalsForPeriod(Tenant tokenTenant, Long startDate,
      Long endDate, UUID organisationId);

  OrganisationVendorPayableResponseDTO getOrganisationPayables(Tenant tokenTenant);

  OrganisationVendorPayableTotalsResponseDTO getOrganisationPayableTotalsForPeriod(Tenant tokenTenant, Long startDate,
      Long endDate, UUID vendorId);
}

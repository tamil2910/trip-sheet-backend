package com.example.trip_sheet_backend.services.InvoiceService;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;

@Service
public class InvoiceServiceImp implements InvoiceService {
  private final InvoiceRepository invoiceRepository;

  public InvoiceServiceImp(InvoiceRepository invoiceRepository) {
    this.invoiceRepository = invoiceRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Invoice markPrinted(UUID invoiceId, Tenant tokenTenant, UUID actorId) {
    Invoice invoice = findAccessibleInvoice(invoiceId, tokenTenant);
    invoice.setIsPrintedInvoice(true);
    setUpdatedBy(invoice, actorId);
    return invoiceRepository.save(invoice);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Invoice markDownloaded(UUID invoiceId, Tenant tokenTenant, UUID actorId) {
    Invoice invoice = findAccessibleInvoice(invoiceId, tokenTenant);
    invoice.setIsDownloadedInvoice(true);
    setUpdatedBy(invoice, actorId);
    return invoiceRepository.save(invoice);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Invoice updateStatus(UUID invoiceId, Invoice.InvoiceStatus status, Tenant tokenTenant, UUID actorId) {
    if (status == null) {
      throw new RuntimeException("Invoice status is required");
    }
    Invoice invoice = findAccessibleInvoice(invoiceId, tokenTenant);
    invoice.setStatus(status);
    setUpdatedBy(invoice, actorId);
    return invoiceRepository.save(invoice);
  }

  private Invoice findAccessibleInvoice(UUID invoiceId, Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    Invoice invoice = invoiceRepository.findById(invoiceId)
        .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Invoice not found"));
    if (sameTenant(tokenTenant, invoice.getTenant())) {
      return invoice;
    }
    if (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getTripSummary() != null
        && invoice.getPurchaseOrder().getTripSummary().getTripId() != null
        && sameTenant(tokenTenant, invoice.getPurchaseOrder().getTripSummary().getTripId().getOrganisation())) {
      return invoice;
    }
    throw new RuntimeException("Invoice is not accessible for this tenant");
  }

  private boolean sameTenant(Tenant first, Tenant second) {
    return first != null && first.getId() != null && second != null && first.getId().equals(second.getId());
  }

  private void setUpdatedBy(Invoice invoice, UUID actorId) {
    if (actorId != null) {
      invoice.setUpdatedBy(actorId.toString());
    }
  }
}

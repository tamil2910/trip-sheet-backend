package com.example.trip_sheet_backend.services.PurchaseInvoiceService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.PurchaseInvoiceRepository;

@Service
public class PurchaseInvoiceServiceImp implements PurchaseInvoiceService {
  private final PurchaseInvoiceRepository repository;
  private final PurchaseInvoiceNumberService purchaseInvoiceNumberService;

  public PurchaseInvoiceServiceImp(PurchaseInvoiceRepository repository,
      PurchaseInvoiceNumberService purchaseInvoiceNumberService) {
    this.repository = repository;
    this.purchaseInvoiceNumberService = purchaseInvoiceNumberService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PurchaseInvoice> getForTenant(Tenant tenant, PurchaseInvoice.PurchaseInvoiceStatus status) {
    requireTenant(tenant);
    return repository.findVisibleToTenant(tenant.getId(), status);
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseInvoice getById(UUID id, Tenant tenant) {
    requireTenant(tenant);
    PurchaseInvoice invoice = repository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new RuntimeException("Purchase invoice not found"));
    if (!canView(tenant, invoice)) {
      throw new RuntimeException("Purchase invoice is not accessible for this tenant");
    }
    return invoice;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public PurchaseInvoice approve(UUID id, Tenant tenant, UUID approvedBy) {
    requireTenant(tenant);
    PurchaseInvoice invoice = repository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new RuntimeException("Purchase invoice not found"));
    if (!sameTenant(tenant, invoice.getPayerVendor())) {
      throw new RuntimeException("Only the paying vendor can approve this purchase invoice");
    }
    if (invoice.getStatus() == PurchaseInvoice.PurchaseInvoiceStatus.CANCELLED) {
      throw new RuntimeException("Cancelled purchase invoices cannot be approved");
    }
    if (invoice.getStatus() == PurchaseInvoice.PurchaseInvoiceStatus.INVOICED
        && invoice.getPurchaseInvoiceNumber() != null && !invoice.getPurchaseInvoiceNumber().isBlank()) {
      return invoice;
    }

    if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()
        || invoice.getStatus() != PurchaseInvoice.PurchaseInvoiceStatus.INVOICE_RAISED) {
      throw new RuntimeException("Vendor invoice must be raised before approving this purchase invoice");
    }

    invoice.setPurchaseInvoiceNumber(purchaseInvoiceNumberService.purchaseInvoiceNumberFor(invoice.getOrderNumber()));
    invoice.setStatus(PurchaseInvoice.PurchaseInvoiceStatus.INVOICED);
    if (approvedBy != null) {
      invoice.setUpdatedBy(approvedBy.toString());
    }
    return repository.save(invoice);
  }

  private void requireTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null || tenant.getTenantType() == null) {
      throw new RuntimeException("Tenant is required to access purchase invoices");
    }
  }

  private boolean canView(Tenant tenant, PurchaseInvoice invoice) {
    return sameTenant(tenant, invoice.getPayerVendor())
        || sameTenant(tenant, invoice.getPayeeVendor())
        || isOwningOrganisation(tenant, invoice);
  }

  private boolean isOwningOrganisation(Tenant tenant, PurchaseInvoice invoice) {
    if (tenant.getTenantType() != Tenant.TenantType.ORGANISATION
        || invoice.getTripSummary() == null
        || invoice.getTripSummary().getTripId() == null) {
      return false;
    }
    return sameTenant(tenant, invoice.getTripSummary().getTripId().getOrganisation());
  }

  private boolean sameTenant(Tenant one, Tenant two) {
    return one != null && one.getId() != null && two != null && one.getId().equals(two.getId());
  }
}

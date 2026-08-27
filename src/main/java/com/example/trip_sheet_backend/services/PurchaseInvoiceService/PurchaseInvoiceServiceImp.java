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

  public PurchaseInvoiceServiceImp(PurchaseInvoiceRepository repository) { this.repository = repository; }

  @Override
  @Transactional(readOnly = true)
  public List<PurchaseInvoice> getForTenant(Tenant tenant) {
    requireVendor(tenant);
    return repository.findVisibleToTenant(tenant.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseInvoice getById(UUID id, Tenant tenant) {
    requireVendor(tenant);
    PurchaseInvoice invoice = repository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new RuntimeException("Purchase invoice not found"));
    if (!sameTenant(tenant, invoice.getPayerVendor()) && !sameTenant(tenant, invoice.getPayeeVendor())) {
      throw new RuntimeException("Purchase invoice is not accessible for this tenant");
    }
    return invoice;
  }

  private void requireVendor(Tenant tenant) {
    if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenants can access purchase invoices");
    }
  }
  private boolean sameTenant(Tenant one, Tenant two) { return two != null && one.getId().equals(two.getId()); }
}

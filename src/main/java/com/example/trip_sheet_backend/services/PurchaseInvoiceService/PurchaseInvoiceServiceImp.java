package com.example.trip_sheet_backend.services.PurchaseInvoiceService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorDelegationHistory;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;
import com.example.trip_sheet_backend.repositories.PurchaseInvoiceRepository;
import com.example.trip_sheet_backend.repositories.VendorDelegationHistoryRepository;
import org.springframework.beans.BeanUtils;

@Service
public class PurchaseInvoiceServiceImp implements PurchaseInvoiceService {
  private final PurchaseInvoiceRepository repository;
  private final PurchaseInvoiceNumberService purchaseInvoiceNumberService;
  private final InvoiceRepository invoiceRepository;
  private final VendorDelegationHistoryRepository delegationHistoryRepository;

  public PurchaseInvoiceServiceImp(PurchaseInvoiceRepository repository,
      PurchaseInvoiceNumberService purchaseInvoiceNumberService, InvoiceRepository invoiceRepository,
      VendorDelegationHistoryRepository delegationHistoryRepository) {
    this.repository = repository;
    this.purchaseInvoiceNumberService = purchaseInvoiceNumberService;
    this.invoiceRepository = invoiceRepository;
    this.delegationHistoryRepository = delegationHistoryRepository;
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
    PurchaseInvoice existing = repository.findByIdAndIsDeletedFalse(id).orElse(null);
    if (existing != null) return approveExisting(existing, tenant, approvedBy);

    Invoice sourceInvoice = invoiceRepository.findById(id)
        .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Purchase invoice source invoice not found"));
    return createFromVendorInvoice(sourceInvoice, tenant, approvedBy);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Invoice> getPendingVendorInvoices(Tenant tenant) {
    requireTenant(tenant);
    if (tenant.getTenantType() != Tenant.TenantType.VENDOR) return List.of();
    return invoiceRepository.findVendorInvoicesPayableBy(tenant.getId()).stream()
        .filter(invoice -> repository.findBySourceInvoice_IdAndIsDeletedFalse(invoice.getId()).isEmpty())
        .toList();
  }

  private PurchaseInvoice approveExisting(PurchaseInvoice invoice, Tenant tenant, UUID approvedBy) {
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

    return invoice;
  }

  private PurchaseInvoice createFromVendorInvoice(Invoice sourceInvoice, Tenant tenant, UUID approvedBy) {
    PurchaseOrder order = sourceInvoice.getPurchaseOrder();
    if (order == null || !sameTenant(tenant, order.getTenant()) || tenant.getTenantType() != Tenant.TenantType.VENDOR
        || order.getSupplierVendor() == null || order.getSupplierVendor().getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only the paying vendor can approve this vendor invoice");
    }
    PurchaseInvoice alreadyCreated = repository.findBySourceInvoice_IdAndIsDeletedFalse(sourceInvoice.getId()).orElse(null);
    if (alreadyCreated != null) return alreadyCreated;
    VendorDelegationHistory delegation = delegationHistoryRepository
        .findByTrip_IdAndIsDeletedFalseOrderByDelegatedAtAscCreatedAtAsc(order.getTripSummary().getTripId().getId()).stream()
        .filter(value -> sameTenant(value.getFromVendor(), tenant) && sameTenant(value.getToVendor(), order.getSupplierVendor()))
        .findFirst().orElseThrow(() -> new RuntimeException("Delegation history not found for vendor invoice"));

    PurchaseInvoice purchaseInvoice = new PurchaseInvoice();
    BeanUtils.copyProperties(order, purchaseInvoice, "id", "createdAt", "updatedAt", "deletedAt", "createdBy", "updatedBy", "deletedBy",
        "isDeleted", "tenant", "status", "allocations", "combinedPurchaseOrder", "supplierVendor");
    purchaseInvoice.setSourceInvoice(sourceInvoice);
    purchaseInvoice.setDelegationHistory(delegation);
    purchaseInvoice.setTripSummary(order.getTripSummary());
    purchaseInvoice.setPayerVendor(tenant);
    purchaseInvoice.setPayeeVendor(order.getSupplierVendor());
    purchaseInvoice.setInvoiceNumber(sourceInvoice.getInvoiceNumber());
    purchaseInvoice.setPurchaseInvoiceNumber(purchaseInvoiceNumberService.purchaseInvoiceNumberFor(order.getOrderNumber()));
    purchaseInvoice.setAmountPayable(order.getTotalAmount());
    purchaseInvoice.setAmountReceivable(order.getTotalAmount());
    purchaseInvoice.setEarning(java.math.BigDecimal.ZERO);
    purchaseInvoice.setStatus(PurchaseInvoice.PurchaseInvoiceStatus.INVOICED);
    if (approvedBy != null) purchaseInvoice.setUpdatedBy(approvedBy.toString());
    return repository.save(purchaseInvoice);
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

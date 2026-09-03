package com.example.trip_sheet_backend.services.InvoiceService;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

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
  @Transactional(readOnly = true)
  public Page<Invoice> getInvoices(Tenant tokenTenant, Map<String, Object> filters, Pageable pageable) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Specification<Invoice> spec = (root, query, cb) -> {
      query.distinct(true);
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.isFalse(root.get("isDeleted")));

      Join<Object, Object> purchaseOrder = root.join("purchaseOrder", JoinType.LEFT);
      Join<Object, Object> tripSummary = purchaseOrder.join("tripSummary", JoinType.LEFT);
      Join<Object, Object> trip = tripSummary.join("tripId", JoinType.LEFT);

      Predicate invoiceTenant = cb.equal(root.join("tenant", JoinType.LEFT).get("id"), tokenTenant.getId());
      Predicate organisation = cb.equal(trip.join("organisation", JoinType.LEFT).get("id"), tokenTenant.getId());
      Predicate vendor = cb.equal(trip.join("vendor", JoinType.LEFT).get("id"), tokenTenant.getId());
      predicates.add(cb.or(invoiceTenant, organisation, vendor));

      addLikeFilter(predicates, cb, trip.get("tripCode"), filters.get("tripCode"));
      addLikeFilter(predicates, cb, trip.join("passengers", JoinType.LEFT).get("name"), filters.get("passengerName"));
      addLikeFilter(predicates, cb, trip.join("driver", JoinType.LEFT).get("fullName"), filters.get("driverName"));
      addLikeFilter(predicates, cb, trip.join("vendor", JoinType.LEFT).get("tenantName"), filters.get("vendorName"));
      addLikeFilter(predicates, cb, trip.join("organisation", JoinType.LEFT).get("tenantName"), filters.get("organisationName"));

      Invoice.InvoiceStatus status = parseStatus(filters.get("invoiceStatus"));
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
    return invoiceRepository.findAll(spec, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Invoice getByPurchaseOrderId(UUID purchaseOrderId, Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    Invoice invoice = invoiceRepository.findLatestByPurchaseOrderId(purchaseOrderId)
        .orElseThrow(() -> new RuntimeException("Invoice not found for this purchase order"));
    validateInvoiceAccess(invoice, tokenTenant);
    return invoice;
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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Invoice cancel(UUID invoiceId, Tenant tokenTenant, UUID actorId) {
    Invoice invoice = findAccessibleInvoice(invoiceId, tokenTenant);
    invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
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
    validateInvoiceAccess(invoice, tokenTenant);
    return invoice;
  }

  private void validateInvoiceAccess(Invoice invoice, Tenant tokenTenant) {
    if (sameTenant(tokenTenant, invoice.getTenant())) {
      return;
    }
    if (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getTripSummary() != null
        && invoice.getPurchaseOrder().getTripSummary().getTripId() != null
        && sameTenant(tokenTenant, invoice.getPurchaseOrder().getTripSummary().getTripId().getOrganisation())) {
      return;
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

  private void addLikeFilter(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
      jakarta.persistence.criteria.Expression<String> field, Object value) {
    if (value == null || value.toString().isBlank()) {
      return;
    }
    predicates.add(cb.like(cb.lower(field), "%" + value.toString().trim().toLowerCase(Locale.ROOT) + "%"));
  }

  private Invoice.InvoiceStatus parseStatus(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    try {
      return Invoice.InvoiceStatus.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new RuntimeException("Invalid invoiceStatus");
    }
  }
}

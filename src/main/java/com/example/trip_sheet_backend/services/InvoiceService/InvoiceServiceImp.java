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

      Join<Object, Object> passengers = trip.join("passengers", JoinType.LEFT);
      Join<Object, Object> driver = trip.join("driver", JoinType.LEFT);
      Join<Object, Object> tripVendor = trip.join("vendor", JoinType.LEFT);
      Join<Object, Object> tripOrganisation = trip.join("organisation", JoinType.LEFT);

      addLikeFilter(predicates, cb, trip.get("tripCode"), filters.get("tripCode"));
      addLikeFilter(predicates, cb, passengers.get("name"), filters.get("passengerName"));
      addLikeFilter(predicates, cb, driver.get("fullName"), filters.get("driverName"));
      addLikeFilter(predicates, cb, tripVendor.get("tenantName"), filters.get("vendorName"));
      addLikeFilter(predicates, cb, tripOrganisation.get("tenantName"), filters.get("organisationName"));

      String searchFilter = firstNonBlank(filters.get("searchFilter"), filters.get("searchValue"));
      if (searchFilter != null) {
        String pattern = "%" + searchFilter.toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(trip.get("tripCode")), pattern),
            cb.like(cb.lower(passengers.get("name")), pattern),
            cb.like(cb.lower(driver.get("fullName")), pattern),
            cb.like(cb.lower(tripVendor.get("tenantName")), pattern),
            cb.like(cb.lower(tripOrganisation.get("tenantName")), pattern)
        ));
      }

      Long startDate = parseLong(filters.get("startDate"), "startDate");
      if (startDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), startDate));
      }
      Long endDate = parseLong(filters.get("endDate"), "endDate");
      if (endDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), endDate));
      }
      Long invoiceDate = parseLong(filters.get("invoiceDate"), "invoiceDate");
      if (invoiceDate != null) {
        predicates.add(cb.equal(root.get("invoiceDate"), invoiceDate));
      }

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

  private String firstNonBlank(Object primary, Object fallback) {
    if (primary != null && !primary.toString().isBlank()) {
      return primary.toString().trim();
    }
    if (fallback != null && !fallback.toString().isBlank()) {
      return fallback.toString().trim();
    }
    return null;
  }

  private Long parseLong(Object value, String fieldName) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException exception) {
      throw new RuntimeException(fieldName + " must be an epoch timestamp in milliseconds");
    }
  }
}

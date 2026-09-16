package com.example.trip_sheet_backend.services.InvoiceService;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.ZoneId;

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
import com.example.trip_sheet_backend.dtos.InvoiceDtos.VendorInvoiceOutstandingResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.OrganisationVendorPayableResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.InvoiceOutstandingTotalsResponseDTO;
import com.example.trip_sheet_backend.dtos.InvoiceDtos.OrganisationVendorPayableTotalsResponseDTO;

@Service
public class InvoiceServiceImp implements InvoiceService {
  private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Kolkata");
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

  @Override
  @Transactional(readOnly = true)
  public VendorInvoiceOutstandingResponseDTO getVendorOutstanding(Tenant tokenTenant) {
    validateVendorTenant(tokenTenant);
    return buildVendorOutstanding(tokenTenant.getId(), invoiceRepository.findOutstandingInvoicesForVendor(tokenTenant.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public VendorInvoiceOutstandingResponseDTO getVendorOutstandingForPeriod(Tenant tokenTenant, Long startDate,
      Long endDate, UUID organisationId) {
    validateVendorTenant(tokenTenant);
    if ((startDate == null) != (endDate == null)) {
      throw new RuntimeException("startDate and endDate must be sent together");
    }
    if (startDate != null && startDate > endDate) {
      throw new RuntimeException("startDate must be before or equal to endDate");
    }
    if (startDate == null) {
      LocalDate month = LocalDate.now(BUSINESS_TIME_ZONE).withDayOfMonth(1);
      startDate = month.atStartOfDay(BUSINESS_TIME_ZONE).toInstant().toEpochMilli();
      endDate = month.plusMonths(1).atStartOfDay(BUSINESS_TIME_ZONE).toInstant().toEpochMilli() - 1;
    }
    return buildVendorOutstanding(tokenTenant.getId(), invoiceRepository.findOutstandingInvoicesForVendorWithinPeriod(
        tokenTenant.getId(), organisationId, startDate, endDate));
  }

  @Override
  @Transactional(readOnly = true)
  public InvoiceOutstandingTotalsResponseDTO getVendorOutstandingTotalsForPeriod(Tenant tokenTenant, Long startDate,
      Long endDate, UUID organisationId) {
    VendorInvoiceOutstandingResponseDTO outstanding = getVendorOutstandingForPeriod(tokenTenant, startDate, endDate,
        organisationId);
    return new InvoiceOutstandingTotalsResponseDTO(outstanding.getTotalInvoiceAmount(),
        outstanding.getTotalReceiptReceived(), outstanding.getTotalCreditDebitNoteApplied(),
        outstanding.getTotalOutstandingAmount());
  }

  private VendorInvoiceOutstandingResponseDTO buildVendorOutstanding(UUID vendorId, List<Invoice> invoices) {
    LinkedHashMap<UUID, OrganisationAccumulator> organisations = new LinkedHashMap<>();
    for (Invoice invoice : invoices) {
      Tenant organisation = invoice.getPurchaseOrder().getTripSummary().getTripId().getOrganisation();
      OrganisationAccumulator group = organisations.computeIfAbsent(organisation.getId(),
          ignored -> new OrganisationAccumulator(organisation));
      BigDecimal invoiceAmount = amount(invoice.getPurchaseOrder().getTotalAmount());
      BigDecimal receipts = amount(invoice.getReceiptAppliedAmount());
      BigDecimal notes = amount(invoice.getCreditDebitNoteAppliedAmount());
      BigDecimal outstanding = invoice.getCurrentPayableAmount() == null ? invoiceAmount : invoice.getCurrentPayableAmount();
      group.add(new VendorInvoiceOutstandingResponseDTO.InvoiceOutstanding(invoice.getId(), invoice.getInvoiceNumber(),
          invoice.getInvoiceDate(), invoiceAmount, receipts, notes, outstanding, outstanding.signum() == 0));
    }
    List<VendorInvoiceOutstandingResponseDTO.OrganisationOutstanding> result = organisations.values().stream()
        .map(OrganisationAccumulator::toResponse).toList();
    return new VendorInvoiceOutstandingResponseDTO(vendorId, sum(result, 0), sum(result, 1), sum(result, 2),
        sum(result, 3), result);
  }

  private void validateVendorTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null || tokenTenant.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenants can view vendor outstanding amounts");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationVendorPayableResponseDTO getOrganisationPayables(Tenant tokenTenant) {
    validateOrganisationTenant(tokenTenant);

    LinkedHashMap<UUID, VendorPayableAccumulator> vendors = new LinkedHashMap<>();
    for (Invoice invoice : invoiceRepository.findOutstandingInvoicesForOrganisation(tokenTenant.getId())) {
      Tenant vendor = invoice.getTenant();
      VendorPayableAccumulator group = vendors.computeIfAbsent(vendor.getId(),
          ignored -> new VendorPayableAccumulator(vendor));
      BigDecimal invoiceAmount = amount(invoice.getPurchaseOrder().getTotalAmount());
      BigDecimal receipts = amount(invoice.getReceiptAppliedAmount());
      BigDecimal notes = amount(invoice.getCreditDebitNoteAppliedAmount());
      BigDecimal payable = invoice.getCurrentPayableAmount() == null ? invoiceAmount : invoice.getCurrentPayableAmount();
      group.add(new OrganisationVendorPayableResponseDTO.InvoicePayable(invoice.getId(), invoice.getInvoiceNumber(),
          invoice.getInvoiceDate(), invoiceAmount, receipts, notes, payable, payable.signum() == 0));
    }

    List<OrganisationVendorPayableResponseDTO.VendorPayable> result = vendors.values().stream()
        .map(VendorPayableAccumulator::toResponse).toList();
    return new OrganisationVendorPayableResponseDTO(tokenTenant.getId(), sumPayables(result, 0),
        sumPayables(result, 1), sumPayables(result, 2), sumPayables(result, 3), result);
  }

  @Override
  @Transactional(readOnly = true)
  public OrganisationVendorPayableTotalsResponseDTO getOrganisationPayableTotalsForPeriod(Tenant tokenTenant,
      Long startDate, Long endDate, UUID vendorId) {
    validateOrganisationTenant(tokenTenant);
    if ((startDate == null) != (endDate == null)) {
      throw new RuntimeException("startDate and endDate must be sent together");
    }
    if (startDate != null && startDate > endDate) {
      throw new RuntimeException("startDate must be before or equal to endDate");
    }
    if (startDate == null) {
      LocalDate month = LocalDate.now(BUSINESS_TIME_ZONE).withDayOfMonth(1);
      startDate = month.atStartOfDay(BUSINESS_TIME_ZONE).toInstant().toEpochMilli();
      endDate = month.plusMonths(1).atStartOfDay(BUSINESS_TIME_ZONE).toInstant().toEpochMilli() - 1;
    }

    BigDecimal totalInvoiceAmount = BigDecimal.ZERO;
    BigDecimal totalReceiptReceived = BigDecimal.ZERO;
    BigDecimal totalCreditDebitNoteApplied = BigDecimal.ZERO;
    BigDecimal totalPayableAmount = BigDecimal.ZERO;
    for (Invoice invoice : invoiceRepository.findOutstandingInvoicesForOrganisationWithinPeriod(tokenTenant.getId(),
        vendorId, startDate, endDate)) {
      BigDecimal invoiceAmount = amount(invoice.getPurchaseOrder().getTotalAmount());
      totalInvoiceAmount = totalInvoiceAmount.add(invoiceAmount);
      totalReceiptReceived = totalReceiptReceived.add(amount(invoice.getReceiptAppliedAmount()));
      totalCreditDebitNoteApplied = totalCreditDebitNoteApplied.add(amount(invoice.getCreditDebitNoteAppliedAmount()));
      totalPayableAmount = totalPayableAmount.add(
          invoice.getCurrentPayableAmount() == null ? invoiceAmount : invoice.getCurrentPayableAmount());
    }
    return new OrganisationVendorPayableTotalsResponseDTO(totalInvoiceAmount, totalReceiptReceived,
        totalCreditDebitNoteApplied, totalPayableAmount);
  }

  private void validateOrganisationTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null
        || tokenTenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
      throw new RuntimeException("Only organisation tenants can view vendor payable amounts");
    }
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal sum(List<VendorInvoiceOutstandingResponseDTO.OrganisationOutstanding> groups, int field) {
    return groups.stream().map(group -> switch (field) {
      case 0 -> group.getTotalInvoiceAmount();
      case 1 -> group.getReceiptReceived();
      case 2 -> group.getCreditDebitNoteApplied();
      default -> group.getOutstandingAmount();
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal sumPayables(List<OrganisationVendorPayableResponseDTO.VendorPayable> groups, int field) {
    return groups.stream().map(group -> switch (field) {
      case 0 -> group.getTotalInvoiceAmount();
      case 1 -> group.getReceiptReceived();
      case 2 -> group.getCreditDebitNoteApplied();
      default -> group.getPayableAmount();
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static class OrganisationAccumulator {
    private final Tenant organisation;
    private final List<VendorInvoiceOutstandingResponseDTO.InvoiceOutstanding> invoices = new ArrayList<>();
    private BigDecimal invoiceAmount = BigDecimal.ZERO;
    private BigDecimal receipts = BigDecimal.ZERO;
    private BigDecimal notes = BigDecimal.ZERO;
    private BigDecimal outstanding = BigDecimal.ZERO;

    private OrganisationAccumulator(Tenant organisation) { this.organisation = organisation; }
    private void add(VendorInvoiceOutstandingResponseDTO.InvoiceOutstanding invoice) {
      invoices.add(invoice);
      invoiceAmount = invoiceAmount.add(invoice.getInvoiceAmount());
      receipts = receipts.add(invoice.getReceiptReceived());
      notes = notes.add(invoice.getCreditDebitNoteApplied());
      outstanding = outstanding.add(invoice.getOutstandingAmount());
    }
    private VendorInvoiceOutstandingResponseDTO.OrganisationOutstanding toResponse() {
      return new VendorInvoiceOutstandingResponseDTO.OrganisationOutstanding(organisation.getId(), organisation.getTenantName(),
          invoiceAmount, receipts, notes, outstanding, invoices);
    }
  }

  private static class VendorPayableAccumulator {
    private final Tenant vendor;
    private final List<OrganisationVendorPayableResponseDTO.InvoicePayable> invoices = new ArrayList<>();
    private BigDecimal invoiceAmount = BigDecimal.ZERO;
    private BigDecimal receipts = BigDecimal.ZERO;
    private BigDecimal notes = BigDecimal.ZERO;
    private BigDecimal payable = BigDecimal.ZERO;

    private VendorPayableAccumulator(Tenant vendor) { this.vendor = vendor; }
    private void add(OrganisationVendorPayableResponseDTO.InvoicePayable invoice) {
      invoices.add(invoice);
      invoiceAmount = invoiceAmount.add(invoice.getInvoiceAmount());
      receipts = receipts.add(invoice.getReceiptReceived());
      notes = notes.add(invoice.getCreditDebitNoteApplied());
      payable = payable.add(invoice.getPayableAmount());
    }
    private OrganisationVendorPayableResponseDTO.VendorPayable toResponse() {
      return new OrganisationVendorPayableResponseDTO.VendorPayable(vendor.getId(), vendor.getTenantName(),
          invoiceAmount, receipts, notes, payable, invoices);
    }
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

package com.example.trip_sheet_backend.services.PurchaseOrderService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderUpdateRequestDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.CombinePurchaseOrdersRequestDTO;
import com.example.trip_sheet_backend.dtos.PurchaseOrderDtos.PurchaseOrderAllocationRequestDTO;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.PurchaseOrderAllocation;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.InvoiceRepository;
import com.example.trip_sheet_backend.repositories.PurchaseOrderRepository;
import com.example.trip_sheet_backend.repositories.PurchaseOrderNumberRuleRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.TripPassengerCustomFieldValueRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;

@Service
public class PurchaseOrderServiceImp implements PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final InvoiceRepository invoiceRepository;
  private final PurchaseOrderNumberRuleRepository purchaseOrderNumberRuleRepository;
  private final TenantRepository tenantRepository;
  private final TripSummaryRepository tripSummaryRepository;
  private final CustomFieldRepository customFieldRepository;
  private final TripPassengerCustomFieldValueRepository tripPassengerCustomFieldValueRepository;

  public PurchaseOrderServiceImp(
      PurchaseOrderRepository purchaseOrderRepository,
      InvoiceRepository invoiceRepository,
      PurchaseOrderNumberRuleRepository purchaseOrderNumberRuleRepository,
      TenantRepository tenantRepository,
      TripSummaryRepository tripSummaryRepository,
      CustomFieldRepository customFieldRepository,
      TripPassengerCustomFieldValueRepository tripPassengerCustomFieldValueRepository
  ) {
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.invoiceRepository = invoiceRepository;
    this.purchaseOrderNumberRuleRepository = purchaseOrderNumberRuleRepository;
    this.tenantRepository = tenantRepository;
    this.tripSummaryRepository = tripSummaryRepository;
    this.customFieldRepository = customFieldRepository;
    this.tripPassengerCustomFieldValueRepository = tripPassengerCustomFieldValueRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PurchaseOrder> getPurchaseOrdersByTenant(Tenant tokenTenant, Pageable pageable) {
    validateTenant(tokenTenant);

    Specification<PurchaseOrder> spec = (root, query, cb) -> {
      query.distinct(true);

      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("isDeleted"), false));

      List<Predicate> visibilityPredicates = new ArrayList<>();
      try {
        visibilityPredicates.add(cb.equal(root.join("tenant", JoinType.LEFT).get("id"), tokenTenant.getId()));
      } catch (Exception ignored) {}

      try {
        Join<Object, Object> tripSummaryJoin = root.join("tripSummary", JoinType.LEFT);
        Join<Object, Object> tripJoin = tripSummaryJoin.join("tripId", JoinType.LEFT);

        visibilityPredicates.add(cb.equal(tripJoin.join("tenant", JoinType.LEFT).get("id"), tokenTenant.getId()));
        visibilityPredicates.add(cb.equal(tripJoin.join("organisation", JoinType.LEFT).get("id"), tokenTenant.getId()));
        visibilityPredicates.add(cb.equal(tripJoin.join("vendor", JoinType.LEFT).get("id"), tokenTenant.getId()));
        visibilityPredicates.add(cb.equal(tripJoin.join("assignedByVendor", JoinType.LEFT).get("id"), tokenTenant.getId()));
        visibilityPredicates.add(cb.equal(tripJoin.join("previousVendor", JoinType.LEFT).get("id"), tokenTenant.getId()));
      } catch (Exception ignored) {}

      if (!visibilityPredicates.isEmpty()) {
        predicates.add(cb.or(visibilityPredicates.toArray(new Predicate[0])));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };

    return purchaseOrderRepository.findAll(spec, pageable);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public PurchaseOrder updatePurchaseOrder(
      UUID purchaseOrderId,
      PurchaseOrderUpdateRequestDTO body,
      Tenant tokenTenant,
      UUID updatedBy
  ) {
    validateTenant(tokenTenant);

    PurchaseOrder purchaseOrder = findByIdAndTenant(purchaseOrderId, tokenTenant);

    if (body.getTripSummaryId() != null) {
      TripSummary tripSummary = tripSummaryRepository.findById(body.getTripSummaryId())
          .orElseThrow(() -> new RuntimeException("Trip summary not found"));
      validateTenantOwnership(tripSummary.getTenant(), tokenTenant, "Trip summary is not accessible for this tenant");
      purchaseOrder.setTripSummary(tripSummary);
    }

    if (body.getAllocations() != null) {
      replaceAllocations(purchaseOrder, body.getAllocations(), updatedBy);
    }

    applyUpdateFields(purchaseOrder, body);
    if (updatedBy != null) {
      purchaseOrder.setUpdatedBy(updatedBy.toString());
    }

    return purchaseOrderRepository.save(purchaseOrder);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deletePurchaseOrder(UUID purchaseOrderId, Tenant tokenTenant, UUID deletedBy) {
    validateTenant(tokenTenant);

    PurchaseOrder purchaseOrder = findByIdAndTenant(purchaseOrderId, tokenTenant);
    purchaseOrder.setIsDeleted(true);
    purchaseOrder.setDeletedAt(System.currentTimeMillis());
    if (deletedBy != null) {
      purchaseOrder.setDeletedBy(deletedBy.toString());
      purchaseOrder.setUpdatedBy(deletedBy.toString());
    }

    purchaseOrderRepository.save(purchaseOrder);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Invoice approvePurchaseOrder(UUID purchaseOrderId, Tenant approvingTenant, UUID approvingUserId) {
    validateTenant(approvingTenant);

    List<Invoice> existingInvoices = invoiceRepository
        .findByPurchaseOrder_IdAndIsDeletedFalseOrderByCreatedAtDesc(purchaseOrderId);
    Invoice existingInvoice = existingInvoices.isEmpty() ? null : existingInvoices.get(0);
    if (existingInvoice != null && existingInvoice.getStatus() != Invoice.InvoiceStatus.CANCELLED) {
      return existingInvoice;
    }

    PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
        .filter(order -> !Boolean.TRUE.equals(order.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Purchase order not found"));
    if (purchaseOrder.getStatus() == PurchaseOrder.PurchaseOrderStatus.REJECTED) {
      throw new RuntimeException("Rejected purchase orders cannot be approved");
    }

    Invoice.ApprovalSide approvalSide = resolveApprovalSide(purchaseOrder, approvingTenant);
    Invoice invoice = new Invoice();
    invoice.setPurchaseOrder(purchaseOrder);
    invoice.setTenant(resolveInvoiceTenant(purchaseOrder, approvingTenant));
    invoice.setInvoiceNumber(buildInvoiceNumber(purchaseOrder, existingInvoices.size()));
    invoice.setStatus(Invoice.InvoiceStatus.GENERATED);
    invoice.setApprovedBySide(approvalSide);
    invoice.setApprovedByUserId(approvingUserId == null ? null : approvingUserId.toString());
    invoice.setApprovedAt(System.currentTimeMillis());
    invoice.setIsPrintedInvoice(false);
    invoice.setIsDownloadedInvoice(false);
    if (approvingUserId != null) {
      invoice.setCreatedBy(approvingUserId.toString());
      invoice.setUpdatedBy(approvingUserId.toString());
    }

    Invoice savedInvoice = invoiceRepository.save(invoice);
    purchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.INVOICED);
    if (approvingUserId != null) {
      purchaseOrder.setUpdatedBy(approvingUserId.toString());
    }
    purchaseOrderRepository.save(purchaseOrder);
    return savedInvoice;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public PurchaseOrder combinePurchaseOrders(CombinePurchaseOrdersRequestDTO body, Tenant vendor, UUID createdBy) {
    validateVendor(vendor);
    Set<UUID> requestedIds = new HashSet<>(body.getPurchaseOrderIds());
    if (requestedIds.size() < 2) throw new RuntimeException("At least two different purchase orders are required");

    List<PurchaseOrder> sourceOrders = purchaseOrderRepository.findAllById(requestedIds);
    if (sourceOrders.size() != requestedIds.size()) throw new RuntimeException("One or more purchase orders were not found");
    PurchaseOrder first = sourceOrders.get(0);
    for (PurchaseOrder source : sourceOrders) validateCombinable(source, first, vendor);

    PurchaseOrder combined = new PurchaseOrder();
    combined.setOrderNumber(nextCombinedOrderNumber(vendor));
    combined.setDocumentType("COMBINED_PO");
    combined.setCurrencyCode(first.getCurrencyCode());
    combined.setTenant(vendor);
    combined.setStatus(PurchaseOrder.PurchaseOrderStatus.GENERATED);
    combined.setDocumentDate(System.currentTimeMillis());
    combined.setDueDate(combined.getDocumentDate());
    combined.setBillingPeriodStart(sourceOrders.stream().map(PurchaseOrder::getBillingPeriodStart).filter(java.util.Objects::nonNull)
        .min(Long::compareTo).orElse(null));
    combined.setBillingPeriodEnd(sourceOrders.stream().map(PurchaseOrder::getBillingPeriodEnd).filter(java.util.Objects::nonNull)
        .max(Long::compareTo).orElse(null));
    copyPartyDetails(first, combined);
    combined.setLineItemCount(sourceOrders.stream().map(PurchaseOrder::getLineItemCount).filter(java.util.Objects::nonNull)
        .mapToInt(Integer::intValue).sum());
    combined.setTaxableSubTotal(sum(sourceOrders, PurchaseOrder::getTaxableSubTotal));
    combined.setGstAmount(sum(sourceOrders, PurchaseOrder::getGstAmount));
    combined.setCgstAmount(sum(sourceOrders, PurchaseOrder::getCgstAmount));
    combined.setSgstAmount(sum(sourceOrders, PurchaseOrder::getSgstAmount));
    combined.setIgstAmount(sum(sourceOrders, PurchaseOrder::getIgstAmount));
    combined.setTaxableTotalWithGst(sum(sourceOrders, PurchaseOrder::getTaxableTotalWithGst));
    combined.setNonTaxableTotal(sum(sourceOrders, PurchaseOrder::getNonTaxableTotal));
    combined.setRoundOffAmount(sum(sourceOrders, PurchaseOrder::getRoundOffAmount));
    combined.setTotalAmount(sum(sourceOrders, PurchaseOrder::getTotalAmount));
    combined.setLineItemsSnapshot("{\"combinedPurchaseOrders\":[" + sourceOrders.stream()
        .map(order -> "\"" + order.getOrderNumber().replace("\"", "\\\"") + "\"")
        .collect(java.util.stream.Collectors.joining(",")) + "]}");
    combined.setNotes("Combined purchase orders: " + sourceOrders.stream().map(PurchaseOrder::getOrderNumber)
        .collect(java.util.stream.Collectors.joining(", ")));
    if (createdBy != null) {
      combined.setCreatedBy(createdBy.toString());
      combined.setUpdatedBy(createdBy.toString());
    }
    purchaseOrderRepository.save(combined);

    for (PurchaseOrder source : sourceOrders) {
      source.setCombinedPurchaseOrder(combined);
      if (createdBy != null) source.setUpdatedBy(createdBy.toString());
    }
    purchaseOrderRepository.saveAll(sourceOrders);
    return combined;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void splitCombinedPurchaseOrder(UUID combinedPurchaseOrderId, Tenant vendor, UUID updatedBy) {
    validateVendor(vendor);
    PurchaseOrder combined = purchaseOrderRepository.findByIdAndTenant_IdAndIsDeletedFalse(combinedPurchaseOrderId, vendor.getId())
        .orElseThrow(() -> new RuntimeException("Combined purchase order not found"));
    if (!"COMBINED_PO".equals(combined.getDocumentType())) throw new RuntimeException("Purchase order is not a combined PO");
    if (combined.getStatus() != PurchaseOrder.PurchaseOrderStatus.GENERATED) {
      throw new RuntimeException("Only generated combined POs can be split");
    }
    List<PurchaseOrder> sourceOrders = purchaseOrderRepository.findByCombinedPurchaseOrder_IdAndIsDeletedFalse(combined.getId());
    if (sourceOrders.isEmpty()) throw new RuntimeException("Combined purchase order has no source POs");
    for (PurchaseOrder source : sourceOrders) {
      if (source.getStatus() != PurchaseOrder.PurchaseOrderStatus.GENERATED) {
        throw new RuntimeException("A combined PO cannot be split after a source PO is processed");
      }
      source.setCombinedPurchaseOrder(null);
      if (updatedBy != null) source.setUpdatedBy(updatedBy.toString());
    }
    combined.setIsDeleted(true);
    combined.setDeletedAt(System.currentTimeMillis());
    if (updatedBy != null) {
      combined.setDeletedBy(updatedBy.toString());
      combined.setUpdatedBy(updatedBy.toString());
    }
    purchaseOrderRepository.saveAll(sourceOrders);
    purchaseOrderRepository.save(combined);
  }

  private void validateCombinable(PurchaseOrder source, PurchaseOrder first, Tenant vendor) {
    if (Boolean.TRUE.equals(source.getIsDeleted()) || source.getCombinedPurchaseOrder() != null) {
      throw new RuntimeException("Purchase order is already combined or deleted: " + source.getId());
    }
    if (source.getStatus() != PurchaseOrder.PurchaseOrderStatus.GENERATED) {
      throw new RuntimeException("Only generated purchase orders can be combined");
    }
    if (source.getTripSummary() == null || source.getTripSummary().getTripId() == null
        || source.getTripSummary().getTripId().getVendor() == null
        || !vendor.getId().equals(source.getTripSummary().getTripId().getVendor().getId())) {
      throw new RuntimeException("Purchase orders can only be combined by their supplier vendor");
    }
    if (first.getTenant() == null || source.getTenant() == null
        || !first.getTenant().getId().equals(source.getTenant().getId())) {
      throw new RuntimeException("Only purchase orders with the same bill-to tenant can be combined");
    }
    if (!java.util.Objects.equals(first.getCurrencyCode(), source.getCurrencyCode())) {
      throw new RuntimeException("Only purchase orders with the same currency can be combined");
    }
  }

  private String nextCombinedOrderNumber(Tenant vendor) {
    Tenant lockedVendor = tenantRepository.findByIdForUpdate(vendor.getId())
        .orElseThrow(() -> new RuntimeException("Vendor not found"));
    com.example.trip_sheet_backend.models.PurchaseOrderNumberRule rule = purchaseOrderNumberRuleRepository
        .findWithLockByVendor_IdAndIsDefaultTrueAndIsDeletedFalse(lockedVendor.getId())
        .orElseGet(() -> createDefaultNumberRule(lockedVendor));
    long sequence = rule.getNextCombinedSequence() == null ? rule.getSequenceStart() : rule.getNextCombinedSequence();
    String number = rule.formatCombinedPurchaseOrderNumber(sequence);
    rule.setNextCombinedSequence(sequence + 1);
    purchaseOrderNumberRuleRepository.save(rule);
    return number;
  }

  private com.example.trip_sheet_backend.models.PurchaseOrderNumberRule createDefaultNumberRule(Tenant vendor) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
    com.example.trip_sheet_backend.models.PurchaseOrderNumberRule rule = new com.example.trip_sheet_backend.models.PurchaseOrderNumberRule();
    rule.setVendor(vendor);
    rule.setPeriod(startYear + "_" + (startYear + 1));
    rule.setSequenceStart(1L);
    rule.setNextSequence(1L);
    rule.setNextCombinedSequence(1L);
    rule.setIsDefault(true);
    return purchaseOrderNumberRuleRepository.saveAndFlush(rule);
  }

  private void copyPartyDetails(PurchaseOrder source, PurchaseOrder target) {
    target.setBillToName(source.getBillToName()); target.setBillToCode(source.getBillToCode());
    target.setBillToGstin(source.getBillToGstin()); target.setBillToAddress(source.getBillToAddress());
    target.setSupplierName(source.getSupplierName()); target.setSupplierPhone(source.getSupplierPhone());
    target.setSupplierAddress(source.getSupplierAddress());
  }

  private BigDecimal sum(List<PurchaseOrder> orders, java.util.function.Function<PurchaseOrder, BigDecimal> field) {
    return orders.stream().map(field).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private void validateVendor(Tenant vendor) {
    if (vendor == null || vendor.getId() == null || vendor.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenants can combine or split purchase orders");
    }
  }

  private void replaceAllocations(
      PurchaseOrder purchaseOrder,
      List<PurchaseOrderAllocationRequestDTO> requestedAllocations,
      UUID updatedBy
  ) {
    if (purchaseOrder.getStatus() == PurchaseOrder.PurchaseOrderStatus.VERIFIED
        || purchaseOrder.getStatus() == PurchaseOrder.PurchaseOrderStatus.INVOICED) {
      throw new RuntimeException("Allocations cannot be changed after the purchase order is verified");
    }
    if (requestedAllocations.isEmpty()) {
      throw new RuntimeException("At least one allocation is required");
    }
    if (purchaseOrder.getTripSummary() == null || purchaseOrder.getTripSummary().getTripId() == null) {
      throw new RuntimeException("Purchase order trip is required for allocation validation");
    }

    BigDecimal totalPercent = requestedAllocations.stream()
        .map(PurchaseOrderAllocationRequestDTO::getSharePercent)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (totalPercent.compareTo(new BigDecimal("100.00")) != 0) {
      throw new RuntimeException("Allocation percentages must total exactly 100.00");
    }

    Set<UUID> customFieldIds = new HashSet<>();
    for (PurchaseOrderAllocationRequestDTO allocation : requestedAllocations) {
      if (allocation.getSharePercent() == null || allocation.getSharePercent().signum() <= 0
          || allocation.getAllocationKey() == null || allocation.getAllocationKey().isBlank()) {
        throw new RuntimeException("Each allocation requires a key and a positive percentage");
      }
      if (allocation.getCustomFieldId() != null) customFieldIds.add(allocation.getCustomFieldId());
    }
    if (customFieldIds.size() > 1 || (customFieldIds.isEmpty() && requestedAllocations.size() > 1)) {
      throw new RuntimeException("All split allocations must use one custom field");
    }

    CustomField customField = null;
    Set<String> allowedKeys = Set.of();
    if (!customFieldIds.isEmpty()) {
      UUID customFieldId = customFieldIds.iterator().next();
      customField = customFieldRepository.findById(customFieldId)
          .orElseThrow(() -> new RuntimeException("Custom field not found"));
      allowedKeys = tripPassengerCustomFieldValueRepository
          .findByTrip_IdAndCustomField_IdAndIsDeletedFalse(purchaseOrder.getTripSummary().getTripId().getId(), customFieldId)
          .stream()
          .map(value -> value.getValue() == null ? "" : value.getValue().trim())
          .filter(value -> !value.isBlank())
          .collect(java.util.stream.Collectors.toSet());
      if (allowedKeys.isEmpty()) {
        throw new RuntimeException("The selected custom field has no values on this trip");
      }
    }

    BigDecimal totalAmount = currency(purchaseOrder.getTotalAmount());
    BigDecimal remainingAmount = totalAmount;
    purchaseOrder.getAllocations().clear();
    for (int index = 0; index < requestedAllocations.size(); index++) {
      PurchaseOrderAllocationRequestDTO request = requestedAllocations.get(index);
      String allocationKey = request.getAllocationKey().trim();
      if (customField != null && (!customField.getId().equals(request.getCustomFieldId()) || !allowedKeys.contains(allocationKey))) {
        throw new RuntimeException("Allocation key does not belong to the selected custom field on this trip: " + allocationKey);
      }

      BigDecimal amount = index == requestedAllocations.size() - 1
          ? remainingAmount
          : currency(totalAmount.multiply(request.getSharePercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
      remainingAmount = remainingAmount.subtract(amount);

      PurchaseOrderAllocation allocation = new PurchaseOrderAllocation();
      allocation.setPurchaseOrder(purchaseOrder);
      allocation.setCustomField(customField);
      allocation.setAllocationKey(allocationKey);
      allocation.setSharePercent(request.getSharePercent().setScale(2, RoundingMode.HALF_UP));
      allocation.setShareAmount(amount);
      if (updatedBy != null) {
        allocation.setCreatedBy(updatedBy.toString());
        allocation.setUpdatedBy(updatedBy.toString());
      }
      purchaseOrder.getAllocations().add(allocation);
    }
  }

  private BigDecimal currency(BigDecimal amount) {
    return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
  }

  private PurchaseOrder findByIdAndTenant(UUID purchaseOrderId, Tenant tokenTenant) {
    return purchaseOrderRepository.findByIdAndTenant_IdAndIsDeletedFalse(purchaseOrderId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Purchase order not found"));
  }

  private Invoice.ApprovalSide resolveApprovalSide(PurchaseOrder purchaseOrder, Tenant approvingTenant) {
    if (purchaseOrder.getTripSummary() != null && purchaseOrder.getTripSummary().getTripId() != null) {
      var trip = purchaseOrder.getTripSummary().getTripId();
      if (sameTenant(approvingTenant, trip.getVendor())) {
        return Invoice.ApprovalSide.VENDOR;
      }
      if (sameTenant(approvingTenant, trip.getOrganisation())) {
        return Invoice.ApprovalSide.ORGANISATION;
      }
    }
    if (sameTenant(approvingTenant, purchaseOrder.getTenant())) {
      return approvingTenant.getTenantType() == Tenant.TenantType.VENDOR
          ? Invoice.ApprovalSide.VENDOR
          : Invoice.ApprovalSide.ORGANISATION;
    }
    throw new RuntimeException("Only the purchase order vendor or organisation can approve it");
  }

  private Tenant resolveInvoiceTenant(PurchaseOrder purchaseOrder, Tenant approvingTenant) {
    if (purchaseOrder.getTripSummary() != null && purchaseOrder.getTripSummary().getTripId() != null
        && purchaseOrder.getTripSummary().getTripId().getVendor() != null) {
      return purchaseOrder.getTripSummary().getTripId().getVendor();
    }
    return approvingTenant;
  }

  private String buildInvoiceNumber(PurchaseOrder purchaseOrder, int previousInvoiceCount) {
    String invoiceNumber = "INV-" + purchaseOrder.getOrderNumber().replaceFirst("^PO-", "");
    return previousInvoiceCount == 0 ? invoiceNumber : invoiceNumber + "-R" + previousInvoiceCount;
  }

  private boolean sameTenant(Tenant first, Tenant second) {
    return first != null && first.getId() != null && second != null && first.getId().equals(second.getId());
  }

  private void applyUpdateFields(PurchaseOrder purchaseOrder, PurchaseOrderUpdateRequestDTO body) {
    if (body.getStatus() != null) purchaseOrder.setStatus(body.getStatus());
    if (body.getOrderNumber() != null) purchaseOrder.setOrderNumber(body.getOrderNumber());
    if (body.getDocumentType() != null) purchaseOrder.setDocumentType(body.getDocumentType());
    if (body.getCurrencyCode() != null) purchaseOrder.setCurrencyCode(body.getCurrencyCode());
    if (body.getDocumentDate() != null) purchaseOrder.setDocumentDate(body.getDocumentDate());
    if (body.getDueDate() != null) purchaseOrder.setDueDate(body.getDueDate());
    if (body.getBillingPeriodStart() != null) purchaseOrder.setBillingPeriodStart(body.getBillingPeriodStart());
    if (body.getBillingPeriodEnd() != null) purchaseOrder.setBillingPeriodEnd(body.getBillingPeriodEnd());
    if (body.getBillToName() != null) purchaseOrder.setBillToName(body.getBillToName());
    if (body.getBillToCode() != null) purchaseOrder.setBillToCode(body.getBillToCode());
    if (body.getBillToGstin() != null) purchaseOrder.setBillToGstin(body.getBillToGstin());
    if (body.getBillToAddress() != null) purchaseOrder.setBillToAddress(body.getBillToAddress());
    if (body.getSupplierName() != null) purchaseOrder.setSupplierName(body.getSupplierName());
    if (body.getSupplierPhone() != null) purchaseOrder.setSupplierPhone(body.getSupplierPhone());
    if (body.getSupplierAddress() != null) purchaseOrder.setSupplierAddress(body.getSupplierAddress());
    if (body.getLineItemCount() != null) purchaseOrder.setLineItemCount(body.getLineItemCount());
    if (body.getLineItemsSnapshot() != null) purchaseOrder.setLineItemsSnapshot(body.getLineItemsSnapshot());
    if (body.getGarageStartTime() != null) purchaseOrder.setGarageStartTime(body.getGarageStartTime());
    if (body.getGarageEndTime() != null) purchaseOrder.setGarageEndTime(body.getGarageEndTime());
    if (body.getTripStartTime() != null) purchaseOrder.setTripStartTime(body.getTripStartTime());
    if (body.getTripStartKmOdo() != null) purchaseOrder.setTripStartKmOdo(body.getTripStartKmOdo());
    if (body.getTripStartKmOdoImage() != null) purchaseOrder.setTripStartKmOdoImage(body.getTripStartKmOdoImage());
    if (body.getTripEndTime() != null) purchaseOrder.setTripEndTime(body.getTripEndTime());
    if (body.getTripEndKmOdo() != null) purchaseOrder.setTripEndKmOdo(body.getTripEndKmOdo());
    if (body.getTripEndKmOdoImage() != null) purchaseOrder.setTripEndKmOdoImage(body.getTripEndKmOdoImage());
    if (body.getTripDuration() != null) purchaseOrder.setTripDuration(body.getTripDuration());
    if (body.getTripDistance() != null) purchaseOrder.setTripDistance(body.getTripDistance());
    if (body.getTripExtraKmOdo() != null) purchaseOrder.setTripExtraKmOdo(body.getTripExtraKmOdo());
    if (body.getTripExtraKm() != null) purchaseOrder.setTripExtraKm(body.getTripExtraKm());
    if (body.getTripExtraHr() != null) purchaseOrder.setTripExtraHr(body.getTripExtraHr());
    if (body.getTripStartGPSKM() != null) purchaseOrder.setTripStartGPSKM(body.getTripStartGPSKM());
    if (body.getTripEndGPSKM() != null) purchaseOrder.setTripEndGPSKM(body.getTripEndGPSKM());
    if (body.getTripGPSDuration() != null) purchaseOrder.setTripGPSDuration(body.getTripGPSDuration());
    if (body.getTripGPSDistance() != null) purchaseOrder.setTripGPSDistance(body.getTripGPSDistance());
    if (body.getDispatchLat() != null) purchaseOrder.setDispatchLat(body.getDispatchLat());
    if (body.getDispatchLng() != null) purchaseOrder.setDispatchLng(body.getDispatchLng());
    if (body.getArrivedLat() != null) purchaseOrder.setArrivedLat(body.getArrivedLat());
    if (body.getArrivedLng() != null) purchaseOrder.setArrivedLng(body.getArrivedLng());
    if (body.getTripStartLat() != null) purchaseOrder.setTripStartLat(body.getTripStartLat());
    if (body.getTripStartLng() != null) purchaseOrder.setTripStartLng(body.getTripStartLng());
    if (body.getTripEndLat() != null) purchaseOrder.setTripEndLat(body.getTripEndLat());
    if (body.getTripEndLng() != null) purchaseOrder.setTripEndLng(body.getTripEndLng());
    if (body.getGarageEndLat() != null) purchaseOrder.setGarageEndLat(body.getGarageEndLat());
    if (body.getGarageEndLng() != null) purchaseOrder.setGarageEndLng(body.getGarageEndLng());
    if (body.getTripCalculationFieldName() != null) purchaseOrder.setTripCalculationFieldName(body.getTripCalculationFieldName());
    if (body.getExtraHrCalculationFieldName() != null) purchaseOrder.setExtraHrCalculationFieldName(body.getExtraHrCalculationFieldName());
    if (body.getExtraKmCalculationFieldName() != null) purchaseOrder.setExtraKmCalculationFieldName(body.getExtraKmCalculationFieldName());
    if (body.getRateCardPackageName() != null) purchaseOrder.setRateCardPackageName(body.getRateCardPackageName());
    if (body.getBaseFareAmount() != null) purchaseOrder.setBaseFareAmount(body.getBaseFareAmount());
    if (body.getBaseFareQty() != null) purchaseOrder.setBaseFareQty(body.getBaseFareQty());
    if (body.getBaseFareTotal() != null) purchaseOrder.setBaseFareTotal(body.getBaseFareTotal());
    if (body.getExtraKmChargeAmount() != null) purchaseOrder.setExtraKmChargeAmount(body.getExtraKmChargeAmount());
    if (body.getExtraKmQty() != null) purchaseOrder.setExtraKmQty(body.getExtraKmQty());
    if (body.getExtraKmTotal() != null) purchaseOrder.setExtraKmTotal(body.getExtraKmTotal());
    if (body.getExtraHrChargeAmount() != null) purchaseOrder.setExtraHrChargeAmount(body.getExtraHrChargeAmount());
    if (body.getExtraHrQty() != null) purchaseOrder.setExtraHrQty(body.getExtraHrQty());
    if (body.getExtraHrTotal() != null) purchaseOrder.setExtraHrTotal(body.getExtraHrTotal());
    if (body.getDailyAllowanceChargeAmount() != null) purchaseOrder.setDailyAllowanceChargeAmount(body.getDailyAllowanceChargeAmount());
    if (body.getDailyAllowanceQty() != null) purchaseOrder.setDailyAllowanceQty(body.getDailyAllowanceQty());
    if (body.getDailyAllowanceTotal() != null) purchaseOrder.setDailyAllowanceTotal(body.getDailyAllowanceTotal());
    if (body.getEarlyAllowanceChargeAmount() != null) purchaseOrder.setEarlyAllowanceChargeAmount(body.getEarlyAllowanceChargeAmount());
    if (body.getEarlyAllowanceQty() != null) purchaseOrder.setEarlyAllowanceQty(body.getEarlyAllowanceQty());
    if (body.getEarlyAllowanceTotal() != null) purchaseOrder.setEarlyAllowanceTotal(body.getEarlyAllowanceTotal());
    if (body.getLateAllowanceChargeAmount() != null) purchaseOrder.setLateAllowanceChargeAmount(body.getLateAllowanceChargeAmount());
    if (body.getLateAllowanceQty() != null) purchaseOrder.setLateAllowanceQty(body.getLateAllowanceQty());
    if (body.getLateAllowanceTotal() != null) purchaseOrder.setLateAllowanceTotal(body.getLateAllowanceTotal());
    if (body.getHourlyAllowanceCharge() != null) purchaseOrder.setHourlyAllowanceCharge(body.getHourlyAllowanceCharge());
    if (body.getHourlyAllowanceQty() != null) purchaseOrder.setHourlyAllowanceQty(body.getHourlyAllowanceQty());
    if (body.getHourlyAllowanceAmount() != null) purchaseOrder.setHourlyAllowanceAmount(body.getHourlyAllowanceAmount());
    if (body.getTollChargeAmount() != null) purchaseOrder.setTollChargeAmount(body.getTollChargeAmount());
    if (body.getTollQty() != null) purchaseOrder.setTollQty(body.getTollQty());
    if (body.getTollTotal() != null) purchaseOrder.setTollTotal(body.getTollTotal());
    if (body.getParkingChargeAmount() != null) purchaseOrder.setParkingChargeAmount(body.getParkingChargeAmount());
    if (body.getParkingQty() != null) purchaseOrder.setParkingQty(body.getParkingQty());
    if (body.getParkingTotal() != null) purchaseOrder.setParkingTotal(body.getParkingTotal());
    if (body.getOtherChargeAmount() != null) purchaseOrder.setOtherChargeAmount(body.getOtherChargeAmount());
    if (body.getOtherQty() != null) purchaseOrder.setOtherQty(body.getOtherQty());
    if (body.getOtherTotal() != null) purchaseOrder.setOtherTotal(body.getOtherTotal());
    if (body.getTaxableSubTotal() != null) purchaseOrder.setTaxableSubTotal(body.getTaxableSubTotal());
    if (body.getGstPercentage() != null) purchaseOrder.setGstPercentage(body.getGstPercentage());
    if (body.getGstAmount() != null) purchaseOrder.setGstAmount(body.getGstAmount());
    if (body.getCgstPercentage() != null) purchaseOrder.setCgstPercentage(body.getCgstPercentage());
    if (body.getCgstAmount() != null) purchaseOrder.setCgstAmount(body.getCgstAmount());
    if (body.getSgstPercentage() != null) purchaseOrder.setSgstPercentage(body.getSgstPercentage());
    if (body.getSgstAmount() != null) purchaseOrder.setSgstAmount(body.getSgstAmount());
    if (body.getIgstPercentage() != null) purchaseOrder.setIgstPercentage(body.getIgstPercentage());
    if (body.getIgstAmount() != null) purchaseOrder.setIgstAmount(body.getIgstAmount());
    if (body.getTaxableTotalWithGst() != null) purchaseOrder.setTaxableTotalWithGst(body.getTaxableTotalWithGst());
    if (body.getNonTaxableTotal() != null) purchaseOrder.setNonTaxableTotal(body.getNonTaxableTotal());
    if (body.getRoundOffAmount() != null) purchaseOrder.setRoundOffAmount(body.getRoundOffAmount());
    if (body.getTotalAmount() != null) purchaseOrder.setTotalAmount(body.getTotalAmount());
    if (body.getNotes() != null) purchaseOrder.setNotes(body.getNotes());
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private void validateTenantOwnership(Tenant resourceTenant, Tenant tokenTenant, String message) {
    if (resourceTenant == null || resourceTenant.getId() == null || !resourceTenant.getId().equals(tokenTenant.getId())) {
      throw new RuntimeException(message);
    }
  }
}

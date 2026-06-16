package com.example.trip_sheet_backend.services.TripBillingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripBillingAllocation;
import com.example.trip_sheet_backend.models.TripBillingRule;
import com.example.trip_sheet_backend.models.TripCharges;
import com.example.trip_sheet_backend.models.TripPassengerCustomFieldValue;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.models.VendorPartnerTax;
import com.example.trip_sheet_backend.repositories.PurchaseOrderRepository;
import com.example.trip_sheet_backend.repositories.TripBillingAllocationRepository;
import com.example.trip_sheet_backend.repositories.TripBillingRuleRepository;
import com.example.trip_sheet_backend.repositories.TripPassengerCustomFieldValueRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationTaxRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerTaxRepository;

@Service
public class TripBillingService {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final TripBillingRuleRepository tripBillingRuleRepository;
  private final TripBillingAllocationRepository tripBillingAllocationRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final TripSummaryRepository tripSummaryRepository;
  private final TripPassengerCustomFieldValueRepository tripPassengerCustomFieldValueRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository;
  private final VendorOrganisationTaxRepository vendorOrganisationTaxRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VendorPartnerRateCardRepository vendorPartnerRateCardRepository;
  private final VendorPartnerTaxRepository vendorPartnerTaxRepository;

  public TripBillingService(
      TripBillingRuleRepository tripBillingRuleRepository,
      TripBillingAllocationRepository tripBillingAllocationRepository,
      PurchaseOrderRepository purchaseOrderRepository,
      TripSummaryRepository tripSummaryRepository,
      TripPassengerCustomFieldValueRepository tripPassengerCustomFieldValueRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository,
      VendorOrganisationTaxRepository vendorOrganisationTaxRepository,
      VendorPartnerRepository vendorPartnerRepository,
      VendorPartnerRateCardRepository vendorPartnerRateCardRepository,
      VendorPartnerTaxRepository vendorPartnerTaxRepository
  ) {
    this.tripBillingRuleRepository = tripBillingRuleRepository;
    this.tripBillingAllocationRepository = tripBillingAllocationRepository;
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.tripSummaryRepository = tripSummaryRepository;
    this.tripPassengerCustomFieldValueRepository = tripPassengerCustomFieldValueRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.vendorOrganisationRateCardRepository = vendorOrganisationRateCardRepository;
    this.vendorOrganisationTaxRepository = vendorOrganisationTaxRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vendorPartnerRateCardRepository = vendorPartnerRateCardRepository;
    this.vendorPartnerTaxRepository = vendorPartnerTaxRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<PurchaseOrder> generatePurchaseOrdersForTrip(Trip trip) {
    if (trip == null || trip.getId() == null) {
      throw new RuntimeException("Trip is required for billing");
    }

    TripSummary tripSummary = tripSummaryRepository.findByTripId_Id(trip.getId())
        .orElseThrow(() -> new RuntimeException("Trip summary not found for billing"));

    TripBillingRule billingRule = resolveBillingRule(trip);
    PricingContext pricingContext = resolvePricingContext(trip);
    ChargeSnapshot chargeSnapshot = buildChargeSnapshot(tripSummary, pricingContext);

    List<TripBillingAllocation> allocations = tripBillingAllocationRepository.findByTrip_IdAndIsDeletedFalse(trip.getId());
    if (allocations.isEmpty()) {
      allocations = createAllocations(trip, billingRule, chargeSnapshot.totalAmount());
    }

    List<PurchaseOrder> generatedPurchaseOrders = new ArrayList<>();
    for (TripBillingAllocation allocation : allocations) {
      if (purchaseOrderRepository.existsByAllocation_IdAndIsDeletedFalse(allocation.getId())) {
        continue;
      }

      PurchaseOrder purchaseOrder = buildPurchaseOrder(trip, tripSummary, allocation, pricingContext, chargeSnapshot);
      generatedPurchaseOrders.add(purchaseOrderRepository.save(purchaseOrder));
    }

    return generatedPurchaseOrders;
  }

  private TripBillingRule resolveBillingRule(Trip trip) {
    UUID tenantId = trip.getOrganisation() != null ? trip.getOrganisation().getId()
        : trip.getTenant() != null ? trip.getTenant().getId() : null;

    if (tenantId == null) {
      throw new RuntimeException("Unable to resolve billing tenant for trip");
    }

    return tripBillingRuleRepository.findFirstByTenant_IdAndActiveTrueAndIsDeletedFalseOrderByUpdatedAtDesc(tenantId)
        .orElseGet(() -> {
          TripBillingRule defaultRule = new TripBillingRule();
          defaultRule.setTenant(trip.getOrganisation() != null ? trip.getOrganisation() : trip.getTenant());
          defaultRule.setBillingBasis(TripBillingRule.BillingBasis.TRIP_WISE);
          defaultRule.setInvoiceGrouping(TripBillingRule.InvoiceGrouping.TRIP);
          defaultRule.setActive(Boolean.TRUE);
          return defaultRule;
        });
  }

  private List<TripBillingAllocation> createAllocations(Trip trip, TripBillingRule billingRule, BigDecimal totalAmount) {
    if (billingRule.getBillingBasis() == TripBillingRule.BillingBasis.CUSTOM_FIELD_WISE
        && billingRule.getCostCenterCustomField() != null
        && billingRule.getCostCenterCustomField().getId() != null) {
      List<TripBillingAllocation> customFieldAllocations = createCustomFieldAllocations(
          trip,
          billingRule.getCostCenterCustomField().getId(),
          totalAmount
      );
      if (!customFieldAllocations.isEmpty()) {
        return tripBillingAllocationRepository.saveAll(customFieldAllocations);
      }
    }

    TripBillingAllocation allocation = new TripBillingAllocation();
    allocation.setTrip(trip);
    allocation.setTenant(resolveAllocationTenant(trip));
    allocation.setAllocationType(TripBillingAllocation.AllocationType.TRIP_WISE);
    allocation.setAllocationKey("FULL_TRIP");
    allocation.setSharePercent(new BigDecimal("100.00"));
    allocation.setShareAmount(scaleCurrency(totalAmount));
    allocation.setStatus(TripBillingAllocation.AllocationStatus.GENERATED);

    return List.of(tripBillingAllocationRepository.save(allocation));
  }

  private List<TripBillingAllocation> createCustomFieldAllocations(Trip trip, UUID customFieldId, BigDecimal totalAmount) {
    List<TripPassengerCustomFieldValue> fieldValues =
        tripPassengerCustomFieldValueRepository.findByTrip_IdAndCustomField_IdAndIsDeletedFalse(trip.getId(), customFieldId);

    Map<String, Integer> groupCounts = new LinkedHashMap<>();
    for (TripPassengerCustomFieldValue fieldValue : fieldValues) {
      String allocationKey = normalizeAllocationKey(fieldValue);
      groupCounts.merge(allocationKey, 1, Integer::sum);
    }

    if (groupCounts.isEmpty()) {
      return List.of();
    }

    int totalPassengers = groupCounts.values().stream().mapToInt(Integer::intValue).sum();
    BigDecimal remainingAmount = scaleCurrency(totalAmount);
    BigDecimal allocatedPercent = ZERO;
    List<TripBillingAllocation> allocations = new ArrayList<>();
    int index = 0;
    int totalGroups = groupCounts.size();

    for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
      index++;
      BigDecimal sharePercent;
      BigDecimal shareAmount;

      if (index == totalGroups) {
        shareAmount = remainingAmount;
        sharePercent = scaleCurrency(new BigDecimal("100.00").subtract(allocatedPercent));
      } else {
        sharePercent = scaleCurrency(
            BigDecimal.valueOf(entry.getValue())
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalPassengers), 2, RoundingMode.HALF_UP)
        );
        shareAmount = scaleCurrency(
            totalAmount.multiply(BigDecimal.valueOf(entry.getValue()))
                .divide(BigDecimal.valueOf(totalPassengers), 2, RoundingMode.HALF_UP)
        );
      }

      remainingAmount = remainingAmount.subtract(shareAmount);
      allocatedPercent = allocatedPercent.add(sharePercent);

      TripBillingAllocation allocation = new TripBillingAllocation();
      allocation.setTrip(trip);
      allocation.setTenant(resolveAllocationTenant(trip));
      allocation.setAllocationType(TripBillingAllocation.AllocationType.CUSTOM_FIELD_WISE);
      allocation.setAllocationKey(entry.getKey());
      allocation.setSharePercent(sharePercent);
      allocation.setShareAmount(shareAmount);
      allocation.setStatus(TripBillingAllocation.AllocationStatus.GENERATED);
      allocations.add(allocation);
    }

    return allocations;
  }

  private Tenant resolveAllocationTenant(Trip trip) {
    return trip.getOrganisation() != null ? trip.getOrganisation() : trip.getTenant();
  }

  private String normalizeAllocationKey(TripPassengerCustomFieldValue fieldValue) {
    if (fieldValue == null) {
      return "UNASSIGNED";
    }
    String rawValue = fieldValue.getValue();
    if (rawValue == null || rawValue.trim().isEmpty()) {
      PeopleTenant passenger = fieldValue.getPassenger();
      if (passenger != null && passenger.getId() != null) {
        return "PASSENGER_" + passenger.getId();
      }
      return "UNASSIGNED";
    }
    return rawValue.trim();
  }

  private PurchaseOrder buildPurchaseOrder(
      Trip trip,
      TripSummary tripSummary,
      TripBillingAllocation allocation,
      PricingContext pricingContext,
      ChargeSnapshot chargeSnapshot
  ) {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setTripSummary(tripSummary);
    purchaseOrder.setAllocation(allocation);
    purchaseOrder.setTenant(resolveAllocationTenant(trip));

    purchaseOrder.setOrderNumber(buildOrderNumber(trip, allocation));
    purchaseOrder.setDocumentType("PO");
    purchaseOrder.setCurrencyCode("INR");

    long now = System.currentTimeMillis();
    purchaseOrder.setDocumentDate(now);
    purchaseOrder.setDueDate(now);
    purchaseOrder.setBillingPeriodStart(trip.getStartDate());
    purchaseOrder.setBillingPeriodEnd(trip.getEndDate());

    purchaseOrder.setBillToName(pricingContext.billTo().getTenantName());
    purchaseOrder.setBillToCode(pricingContext.billTo().getTenantUniqueCode());
    purchaseOrder.setBillToGstin(pricingContext.billTo().getGstNumber());
    purchaseOrder.setBillToAddress(pricingContext.billTo().getAddress());

    purchaseOrder.setSupplierName(pricingContext.supplier().getTenantName());
    purchaseOrder.setSupplierPhone(pricingContext.supplier().getContactEmail());
    purchaseOrder.setSupplierAddress(pricingContext.supplier().getAddress());

    purchaseOrder.setGarageStartTime(tripSummary.getGarageStartTime());
    purchaseOrder.setGarageEndTime(tripSummary.getGarageEndTime());
    purchaseOrder.setTripStartTime(tripSummary.getTripStartTime());
    purchaseOrder.setTripStartKmOdo(tripSummary.getTripStartKmOdo());
    purchaseOrder.setTripStartKmOdoImage(tripSummary.getTripStartKmOdoImage());
    purchaseOrder.setTripEndTime(tripSummary.getTripEndTime());
    purchaseOrder.setTripEndKmOdo(tripSummary.getTripEndKmOdo());
    purchaseOrder.setTripEndKmOdoImage(tripSummary.getTripEndKmOdoImage());
    purchaseOrder.setTripDuration(tripSummary.getTripDuration());
    purchaseOrder.setTripDistance(tripSummary.getTripDistance());
    purchaseOrder.setTripExtraKmOdo(tripSummary.getTripExtraKmOdo());
    purchaseOrder.setTripExtraKm(tripSummary.getTripExtraKm());
    purchaseOrder.setTripExtraHr(tripSummary.getTripExtraHr());
    purchaseOrder.setTripStartGPSKM(tripSummary.getTripStartGPSKM());
    purchaseOrder.setTripEndGPSKM(tripSummary.getTripEndGPSKM());
    purchaseOrder.setTripGPSDuration(tripSummary.getTripGPSDuration());
    purchaseOrder.setTripGPSDistance(tripSummary.getTripGPSDistance());
    purchaseOrder.setDispatchLat(tripSummary.getDispatchLat());
    purchaseOrder.setDispatchLng(tripSummary.getDispatchLng());
    purchaseOrder.setArrivedLat(tripSummary.getArrivedLat());
    purchaseOrder.setArrivedLng(tripSummary.getArrivedLng());
    purchaseOrder.setTripStartLat(tripSummary.getTripStartLat());
    purchaseOrder.setTripStartLng(tripSummary.getTripStartLng());
    purchaseOrder.setTripEndLat(tripSummary.getTripEndLat());
    purchaseOrder.setTripEndLng(tripSummary.getTripEndLng());
    purchaseOrder.setGarageEndLat(tripSummary.getGarageEndLat());
    purchaseOrder.setGarageEndLng(tripSummary.getGarageEndLng());

    purchaseOrder.setTripCalculationFieldName(pricingContext.dutyTypeName());
    purchaseOrder.setExtraHrCalculationFieldName("tripExtraHr");
    purchaseOrder.setExtraKmCalculationFieldName("tripExtraKm");
    purchaseOrder.setRateCardPackageName(pricingContext.rateCardPackageName());

    purchaseOrder.setBaseFareAmount(chargeSnapshot.baseFareAmount());
    purchaseOrder.setBaseFareQty(chargeSnapshot.baseFareQty());
    purchaseOrder.setBaseFareTotal(allocateAmount(chargeSnapshot.baseFareTotal(), allocation, chargeSnapshot.totalAmount()));

    purchaseOrder.setExtraKmChargeAmount(chargeSnapshot.extraKmAmount());
    purchaseOrder.setExtraKmQty(chargeSnapshot.extraKmQty());
    purchaseOrder.setExtraKmTotal(allocateAmount(chargeSnapshot.extraKmTotal(), allocation, chargeSnapshot.totalAmount()));

    purchaseOrder.setExtraHrChargeAmount(chargeSnapshot.extraHrAmount());
    purchaseOrder.setExtraHrQty(chargeSnapshot.extraHrQty());
    purchaseOrder.setExtraHrTotal(allocateAmount(chargeSnapshot.extraHrTotal(), allocation, chargeSnapshot.totalAmount()));

    purchaseOrder.setTollChargeAmount(chargeSnapshot.tollAmount());
    purchaseOrder.setTollQty(chargeSnapshot.tollQty());
    purchaseOrder.setTollTotal(allocateAmount(chargeSnapshot.tollTotal(), allocation, chargeSnapshot.totalAmount()));

    purchaseOrder.setParkingChargeAmount(chargeSnapshot.parkingAmount());
    purchaseOrder.setParkingQty(chargeSnapshot.parkingQty());
    purchaseOrder.setParkingTotal(allocateAmount(chargeSnapshot.parkingTotal(), allocation, chargeSnapshot.totalAmount()));

    purchaseOrder.setOtherChargeAmount(chargeSnapshot.otherAmount());
    purchaseOrder.setOtherQty(chargeSnapshot.otherQty());
    purchaseOrder.setOtherTotal(allocateAmount(chargeSnapshot.otherTotal(), allocation, chargeSnapshot.totalAmount()));

    BigDecimal taxableSubTotal = sum(
        purchaseOrder.getBaseFareTotal(),
        purchaseOrder.getExtraKmTotal(),
        purchaseOrder.getExtraHrTotal()
    );
    BigDecimal nonTaxableTotal = sum(
        purchaseOrder.getTollTotal(),
        purchaseOrder.getParkingTotal(),
        purchaseOrder.getOtherTotal()
    );

    purchaseOrder.setTaxableSubTotal(taxableSubTotal);
    applyTaxes(purchaseOrder, pricingContext.taxRates(), taxableSubTotal);
    purchaseOrder.setTaxableTotalWithGst(sum(taxableSubTotal, purchaseOrder.getGstAmount()));
    purchaseOrder.setNonTaxableTotal(nonTaxableTotal);
    purchaseOrder.setRoundOffAmount(ZERO);
    purchaseOrder.setTotalAmount(sum(purchaseOrder.getTaxableTotalWithGst(), nonTaxableTotal));

    purchaseOrder.setLineItemCount(buildLineItemCount(purchaseOrder));
    purchaseOrder.setLineItemsSnapshot(buildLineItemSnapshot(purchaseOrder, allocation));
    purchaseOrder.setNotes(pricingContext.notes() + " | Allocation: " + allocation.getAllocationKey());

    return purchaseOrder;
  }

  private BigDecimal allocateAmount(BigDecimal amount, TripBillingAllocation allocation, BigDecimal totalAmount) {
    if (amount == null || allocation == null || totalAmount == null || totalAmount.signum() == 0) {
      return scaleCurrency(amount);
    }
    return scaleCurrency(
        amount.multiply(allocation.getShareAmount())
            .divide(totalAmount, 2, RoundingMode.HALF_UP)
    );
  }

  private void applyTaxes(PurchaseOrder purchaseOrder, TaxRateSummary taxRateSummary, BigDecimal taxableSubTotal) {
    BigDecimal cgstPercentage = scaleCurrency(taxRateSummary.cgstPercentage());
    BigDecimal sgstPercentage = scaleCurrency(taxRateSummary.sgstPercentage());
    BigDecimal igstPercentage = scaleCurrency(taxRateSummary.igstPercentage());

    BigDecimal cgstAmount = calculateTaxAmount(taxableSubTotal, cgstPercentage);
    BigDecimal sgstAmount = calculateTaxAmount(taxableSubTotal, sgstPercentage);
    BigDecimal igstAmount = calculateTaxAmount(taxableSubTotal, igstPercentage);

    purchaseOrder.setCgstPercentage(cgstPercentage);
    purchaseOrder.setCgstAmount(cgstAmount);
    purchaseOrder.setSgstPercentage(sgstPercentage);
    purchaseOrder.setSgstAmount(sgstAmount);
    purchaseOrder.setIgstPercentage(igstPercentage);
    purchaseOrder.setIgstAmount(igstAmount);

    BigDecimal gstPercentage = sum(cgstPercentage, sgstPercentage, igstPercentage);
    BigDecimal gstAmount = sum(cgstAmount, sgstAmount, igstAmount);

    purchaseOrder.setGstPercentage(gstPercentage);
    purchaseOrder.setGstAmount(gstAmount);
  }

  private BigDecimal calculateTaxAmount(BigDecimal taxableSubTotal, BigDecimal percentage) {
    if (taxableSubTotal == null || percentage == null || percentage.signum() == 0) {
      return ZERO;
    }
    return scaleCurrency(taxableSubTotal.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
  }

  private Integer buildLineItemCount(PurchaseOrder purchaseOrder) {
    int count = 0;
    if (positive(purchaseOrder.getBaseFareTotal())) {
      count++;
    }
    if (positive(purchaseOrder.getExtraKmTotal())) {
      count++;
    }
    if (positive(purchaseOrder.getExtraHrTotal())) {
      count++;
    }
    if (positive(purchaseOrder.getTollTotal())) {
      count++;
    }
    if (positive(purchaseOrder.getParkingTotal())) {
      count++;
    }
    if (positive(purchaseOrder.getOtherTotal())) {
      count++;
    }
    return count;
  }

  private String buildLineItemSnapshot(PurchaseOrder purchaseOrder, TripBillingAllocation allocation) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    appendLineItem(builder, "baseFare", purchaseOrder.getBaseFareQty(), purchaseOrder.getBaseFareAmount(), purchaseOrder.getBaseFareTotal());
    appendLineItem(builder, "extraKm", purchaseOrder.getExtraKmQty(), purchaseOrder.getExtraKmChargeAmount(), purchaseOrder.getExtraKmTotal());
    appendLineItem(builder, "extraHr", purchaseOrder.getExtraHrQty(), purchaseOrder.getExtraHrChargeAmount(), purchaseOrder.getExtraHrTotal());
    appendLineItem(builder, "toll", purchaseOrder.getTollQty(), purchaseOrder.getTollChargeAmount(), purchaseOrder.getTollTotal());
    appendLineItem(builder, "parking", purchaseOrder.getParkingQty(), purchaseOrder.getParkingChargeAmount(), purchaseOrder.getParkingTotal());
    appendLineItem(builder, "other", purchaseOrder.getOtherQty(), purchaseOrder.getOtherChargeAmount(), purchaseOrder.getOtherTotal());
    if (builder.length() > 1 && builder.charAt(builder.length() - 1) == ',') {
      builder.deleteCharAt(builder.length() - 1);
    }
    builder.append("]");
    return "{\"allocationKey\":\"" + allocation.getAllocationKey() + "\",\"items\":" + builder + "}";
  }

  private void appendLineItem(StringBuilder builder, String label, BigDecimal qty, BigDecimal amount, BigDecimal total) {
    if (!positive(total)) {
      return;
    }
    builder.append("{\"label\":\"")
        .append(label)
        .append("\",\"qty\":\"")
        .append(scaleCurrency(qty).toPlainString())
        .append("\",\"amount\":\"")
        .append(scaleCurrency(amount).toPlainString())
        .append("\",\"total\":\"")
        .append(scaleCurrency(total).toPlainString())
        .append("\"},");
  }

  private String buildOrderNumber(Trip trip, TripBillingAllocation allocation) {
    String tripCode = trip.getTripCode() != null && !trip.getTripCode().isBlank() ? trip.getTripCode() : trip.getId().toString();
    String allocationSuffix = allocation.getAllocationKey() == null ? "FULL_TRIP"
        : allocation.getAllocationKey().replaceAll("[^A-Za-z0-9]", "_").toUpperCase(Locale.ROOT);
    return "PO-" + tripCode + "-" + allocationSuffix;
  }

  private PricingContext resolvePricingContext(Trip trip) {
    if (trip.getVendor() == null) {
      throw new RuntimeException("Trip vendor is required for billing");
    }
    if (trip.getAssignedByVendor() != null
        && trip.getAssignedByVendor().getId() != null
        && !Objects.equals(trip.getAssignedByVendor().getId(), trip.getVendor().getId())) {
      return resolvePartnerPricingContext(trip);
    }
    return resolveOrganisationPricingContext(trip);
  }

  private PricingContext resolveOrganisationPricingContext(Trip trip) {
    if (trip.getOrganisation() == null || trip.getOrganisation().getId() == null) {
      throw new RuntimeException("Trip organisation is required for vendor to organisation billing");
    }

    VendorOrganisation vendorOrganisation = vendorOrganisationRepository
        .findByVendorAndOrganisation_Id(trip.getVendor(), trip.getOrganisation().getId())
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found for trip"));

    if (vendorOrganisation.getContractStatus() != VendorOrganisation.ContractStatus.ACTIVE) {
      throw new RuntimeException("Vendor organisation contract is not active for trip billing");
    }

    VendorOrganisationRateCard rateCard = vendorOrganisationRateCardRepository
        .findByVendorOrganisationIdAndIsDeletedFalse(vendorOrganisation.getId())
        .stream()
        .filter(card -> card.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.APPROVED)
        .filter(card -> matchesRateCard(card.getVehicleType(), card.getDutyType(), trip))
        .max(Comparator.comparing(card -> card.getApprovedAt() == null ? 0L : card.getApprovedAt()))
        .orElseThrow(() -> new RuntimeException("No approved vendor organisation rate card found for trip"));

    List<VendorOrganisationTax> taxes =
        vendorOrganisationTaxRepository.findByVendorOrganisation_IdAndIsDeletedFalse(vendorOrganisation.getId());

    return new PricingContext(
        trip.getOrganisation(),
        trip.getVendor(),
        rateCard.getBaseFare(),
        rateCard.getExtraKmCharges(),
        rateCard.getExtraHrCharges(),
        buildTaxRateSummaryFromVendorOrganisationTaxes(taxes),
        trip.getDutyType() == null ? null : trip.getDutyType().getName(),
        buildRateCardPackageName(rateCard.getCity(), trip.getVehicleType(), trip.getDutyType()),
        "Vendor to organisation billing"
    );
  }

  private PricingContext resolvePartnerPricingContext(Trip trip) {
    VendorPartner vendorPartner = vendorPartnerRepository
        .findByPrimaryVendorAndPartnerVendor(trip.getAssignedByVendor(), trip.getVendor())
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found for trip"));

    if (vendorPartner.getContractStatus() != VendorPartner.ContractStatus.ACTIVE) {
      throw new RuntimeException("Vendor partner contract is not active for trip billing");
    }

    VendorPartnerRateCard rateCard = vendorPartnerRateCardRepository
        .findByVendorPartnerIdAndIsDeletedFalse(vendorPartner.getId())
        .stream()
        .filter(card -> card.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.APPROVED)
        .filter(card -> matchesRateCard(card.getVehicleType(), card.getDutyType(), trip))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No approved vendor partner rate card found for trip"));

    List<VendorPartnerTax> taxes =
        vendorPartnerTaxRepository.findByVendorPartner_IdAndIsDeletedFalse(vendorPartner.getId());

    return new PricingContext(
        vendorPartner.getPrimaryVendor(),
        vendorPartner.getPartnerVendor(),
        rateCard.getBaseFare(),
        rateCard.getExtraKmCharges(),
        rateCard.getExtraHrCharges(),
        buildTaxRateSummaryFromVendorPartnerTaxes(taxes),
        trip.getDutyType() == null ? null : trip.getDutyType().getName(),
        buildRateCardPackageName(rateCard.getCity(), trip.getVehicleType(), trip.getDutyType()),
        "Partner vendor to primary vendor billing"
    );
  }

  private boolean matchesRateCard(com.example.trip_sheet_backend.models.VehicleType rateCardVehicleType, DutyType rateCardDutyType, Trip trip) {
    if (trip.getVehicleType() == null || trip.getDutyType() == null) {
      return false;
    }
    return rateCardVehicleType != null
        && rateCardDutyType != null
        && Objects.equals(rateCardVehicleType.getId(), trip.getVehicleType().getId())
        && Objects.equals(rateCardDutyType.getId(), trip.getDutyType().getId());
  }

  private String buildRateCardPackageName(String city, com.example.trip_sheet_backend.models.VehicleType vehicleType, DutyType dutyType) {
    String vehicleName = vehicleType == null ? "vehicle" : vehicleType.getDefaultName();
    String dutyTypeName = dutyType == null ? "duty" : dutyType.getName();
    String cityPart = city == null || city.isBlank() ? "default-city" : city.trim();
    return cityPart + " | " + vehicleName + " | " + dutyTypeName;
  }

  private TaxRateSummary buildTaxRateSummaryFromVendorOrganisationTaxes(List<VendorOrganisationTax> relationTaxes) {
    TaxRateSummary summary = new TaxRateSummary(ZERO, ZERO, ZERO);
    for (VendorOrganisationTax relationTax : relationTaxes) {
      summary = addTax(summary, relationTax.getTax());
    }
    return summary;
  }

  private TaxRateSummary buildTaxRateSummaryFromVendorPartnerTaxes(List<VendorPartnerTax> relationTaxes) {
    TaxRateSummary summary = new TaxRateSummary(ZERO, ZERO, ZERO);
    for (VendorPartnerTax relationTax : relationTaxes) {
      summary = addTax(summary, relationTax.getTax());
    }
    return summary;
  }

  private TaxRateSummary addTax(TaxRateSummary current, Tax tax) {
    if (tax == null || tax.getTaxType() == null || tax.getTaxPercentage() == null) {
      return current;
    }

    return switch (tax.getTaxType()) {
      case CGST -> new TaxRateSummary(sum(current.cgstPercentage(), tax.getTaxPercentage()), current.sgstPercentage(), current.igstPercentage());
      case SGST -> new TaxRateSummary(current.cgstPercentage(), sum(current.sgstPercentage(), tax.getTaxPercentage()), current.igstPercentage());
      case IGST -> new TaxRateSummary(current.cgstPercentage(), current.sgstPercentage(), sum(current.igstPercentage(), tax.getTaxPercentage()));
    };
  }

  private ChargeSnapshot buildChargeSnapshot(TripSummary tripSummary, PricingContext pricingContext) {
    BigDecimal baseFareAmount = scaleCurrency(pricingContext.baseFare());
    BigDecimal baseFareQty = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
    BigDecimal baseFareTotal = scaleCurrency(baseFareAmount.multiply(baseFareQty));

    BigDecimal extraKmQty = scaleNumber(tripSummary.getTripExtraKm());
    BigDecimal extraKmAmount = scaleCurrency(pricingContext.extraKmCharge());
    BigDecimal extraKmTotal = scaleCurrency(extraKmAmount.multiply(extraKmQty));

    BigDecimal extraHrQty = scaleNumber(tripSummary.getTripExtraHr());
    BigDecimal extraHrAmount = scaleCurrency(pricingContext.extraHrCharge());
    BigDecimal extraHrTotal = scaleCurrency(extraHrAmount.multiply(extraHrQty));

    BigDecimal tollTotal = ZERO;
    BigDecimal parkingTotal = ZERO;
    BigDecimal otherTotal = ZERO;
    for (TripCharges charge : tripSummary.getTripCharges()) {
      if (charge == null || charge.getAmount() == null || charge.getType() == null) {
        continue;
      }
      BigDecimal amount = scaleCurrency(BigDecimal.valueOf(charge.getAmount()));
      switch (charge.getType()) {
        case Toll -> tollTotal = tollTotal.add(amount);
        case Parking -> parkingTotal = parkingTotal.add(amount);
        case Other -> otherTotal = otherTotal.add(amount);
      }
    }

    BigDecimal totalAmount = sum(baseFareTotal, extraKmTotal, extraHrTotal, tollTotal, parkingTotal, otherTotal);

    return new ChargeSnapshot(
        baseFareAmount,
        baseFareQty,
        baseFareTotal,
        extraKmAmount,
        extraKmQty,
        extraKmTotal,
        extraHrAmount,
        extraHrQty,
        extraHrTotal,
        positive(tollTotal) ? tollTotal : ZERO,
        positive(tollTotal) ? BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP) : ZERO,
        positive(parkingTotal) ? parkingTotal : ZERO,
        positive(parkingTotal) ? BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP) : ZERO,
        positive(otherTotal) ? otherTotal : ZERO,
        positive(otherTotal) ? BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP) : ZERO,
        totalAmount
    );
  }

  private BigDecimal scaleCurrency(BigDecimal value) {
    if (value == null) {
      return ZERO;
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal scaleNumber(Long value) {
    if (value == null || value < 0) {
      return ZERO;
    }
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal sum(BigDecimal... values) {
    BigDecimal total = ZERO;
    if (values == null) {
      return total;
    }
    for (BigDecimal value : values) {
      if (value != null) {
        total = total.add(value);
      }
    }
    return scaleCurrency(total);
  }

  private boolean positive(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }

  private record PricingContext(
      Tenant billTo,
      Tenant supplier,
      BigDecimal baseFare,
      BigDecimal extraKmCharge,
      BigDecimal extraHrCharge,
      TaxRateSummary taxRates,
      String dutyTypeName,
      String rateCardPackageName,
      String notes
  ) {
  }

  private record TaxRateSummary(
      BigDecimal cgstPercentage,
      BigDecimal sgstPercentage,
      BigDecimal igstPercentage
  ) {
  }

  private record ChargeSnapshot(
      BigDecimal baseFareAmount,
      BigDecimal baseFareQty,
      BigDecimal baseFareTotal,
      BigDecimal extraKmAmount,
      BigDecimal extraKmQty,
      BigDecimal extraKmTotal,
      BigDecimal extraHrAmount,
      BigDecimal extraHrQty,
      BigDecimal extraHrTotal,
      BigDecimal tollAmount,
      BigDecimal tollQty,
      BigDecimal parkingAmount,
      BigDecimal parkingQty,
      BigDecimal otherAmount,
      BigDecimal otherQty,
      BigDecimal totalAmount
  ) {
    private BigDecimal tollTotal() {
      return tollAmount;
    }

    private BigDecimal parkingTotal() {
      return parkingAmount;
    }

    private BigDecimal otherTotal() {
      return otherAmount;
    }
  }
}

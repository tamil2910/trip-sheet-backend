package com.example.trip_sheet_backend.services.TripBillingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.CustomTax;
import com.example.trip_sheet_backend.models.PurchaseOrder;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Trip;
import com.example.trip_sheet_backend.models.TripCharges;
import com.example.trip_sheet_backend.models.TripSummary;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.repositories.PurchaseOrderRepository;
import com.example.trip_sheet_backend.repositories.CustomTaxRepository;
import com.example.trip_sheet_backend.repositories.TripRepository;
import com.example.trip_sheet_backend.repositories.TripSummaryRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class TripBillingService {

  private static final Logger log = LoggerFactory.getLogger(TripBillingService.class);

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final TripSummaryRepository tripSummaryRepository;
  private final TripRepository tripRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository;
  private final CustomTaxRepository customTaxRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VendorPartnerRateCardRepository vendorPartnerRateCardRepository;

  public TripBillingService(
      PurchaseOrderRepository purchaseOrderRepository,
      TripSummaryRepository tripSummaryRepository,
      TripRepository tripRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository,
      CustomTaxRepository customTaxRepository,
      VendorPartnerRepository vendorPartnerRepository,
      VendorPartnerRateCardRepository vendorPartnerRateCardRepository
  ) {
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.tripSummaryRepository = tripSummaryRepository;
    this.tripRepository = tripRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.vendorOrganisationRateCardRepository = vendorOrganisationRateCardRepository;
    this.customTaxRepository = customTaxRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vendorPartnerRateCardRepository = vendorPartnerRateCardRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  public List<PurchaseOrder> generatePurchaseOrdersForTrip(UUID tripId) {
    if (tripId == null) {
      throw new RuntimeException("Trip is required for billing");
    }

    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new RuntimeException("Trip not found for billing"));

    TripSummary tripSummary = tripSummaryRepository.findByTripId_Id(trip.getId())
        .orElseThrow(() -> new RuntimeException("Trip summary not found for billing"));

    PricingContext pricingContext = resolvePricingContext(trip);
    ChargeSnapshot chargeSnapshot = buildChargeSnapshot(tripSummary, pricingContext);

    if (purchaseOrderRepository.existsByTripSummary_IdAndIsDeletedFalse(tripSummary.getId())) {
      return List.of();
    }

    PurchaseOrder purchaseOrder = buildPurchaseOrder(trip, tripSummary, pricingContext, chargeSnapshot);
    return List.of(purchaseOrderRepository.save(purchaseOrder));
  }

  private PurchaseOrder buildPurchaseOrder(
      Trip trip,
      TripSummary tripSummary,
      PricingContext pricingContext,
      ChargeSnapshot chargeSnapshot
  ) {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setTripSummary(tripSummary);
    purchaseOrder.setTenant(trip.getOrganisation() != null ? trip.getOrganisation() : trip.getTenant());
    purchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.GENERATED);

    purchaseOrder.setOrderNumber(buildOrderNumber(trip));
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
    purchaseOrder.setBaseFareTotal(chargeSnapshot.baseFareTotal());

    purchaseOrder.setExtraKmChargeAmount(chargeSnapshot.extraKmAmount());
    purchaseOrder.setExtraKmQty(chargeSnapshot.extraKmQty());
    purchaseOrder.setExtraKmTotal(chargeSnapshot.extraKmTotal());

    purchaseOrder.setExtraHrChargeAmount(chargeSnapshot.extraHrAmount());
    purchaseOrder.setExtraHrQty(chargeSnapshot.extraHrQty());
    purchaseOrder.setExtraHrTotal(chargeSnapshot.extraHrTotal());

    purchaseOrder.setTollChargeAmount(chargeSnapshot.tollAmount());
    purchaseOrder.setTollQty(chargeSnapshot.tollQty());
    purchaseOrder.setTollTotal(chargeSnapshot.tollTotal());

    purchaseOrder.setParkingChargeAmount(chargeSnapshot.parkingAmount());
    purchaseOrder.setParkingQty(chargeSnapshot.parkingQty());
    purchaseOrder.setParkingTotal(chargeSnapshot.parkingTotal());

    purchaseOrder.setOtherChargeAmount(chargeSnapshot.otherAmount());
    purchaseOrder.setOtherQty(chargeSnapshot.otherQty());
    purchaseOrder.setOtherTotal(chargeSnapshot.otherTotal());

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
    purchaseOrder.setLineItemsSnapshot(buildLineItemSnapshot(purchaseOrder));
    purchaseOrder.setNotes(pricingContext.notes());

    return purchaseOrder;
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

  private String buildLineItemSnapshot(PurchaseOrder purchaseOrder) {
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
    return "{\"items\":" + builder + "}";
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

  private String buildOrderNumber(Trip trip) {
    String tripCode = trip.getTripCode() != null && !trip.getTripCode().isBlank() ? trip.getTripCode() : trip.getId().toString();
    return "PO-" + tripCode;
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

    List<CustomTax> taxes = customTaxRepository
        .findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(trip.getVendor().getId());

    return new PricingContext(
        trip.getOrganisation(),
        trip.getVendor(),
        rateCard.getBaseFare(),
        rateCard.getExtraKmCharges(),
        rateCard.getExtraHrCharges(),
        buildTaxRateSummary(taxes),
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

    List<CustomTax> taxes = customTaxRepository
        .findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(vendorPartner.getPrimaryVendor().getId());

    return new PricingContext(
        vendorPartner.getPrimaryVendor(),
        vendorPartner.getPartnerVendor(),
        rateCard.getBaseFare(),
        rateCard.getExtraKmCharges(),
        rateCard.getExtraHrCharges(),
        buildTaxRateSummary(taxes),
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

  private TaxRateSummary buildTaxRateSummary(List<CustomTax> customTaxes) {
    TaxRateSummary summary = new TaxRateSummary(ZERO, ZERO, ZERO);
    for (CustomTax customTax : customTaxes) {
      if (Boolean.FALSE.equals(customTax.getIsActive())) {
        continue;
      }
      summary = addTax(summary, customTax.getTax());
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

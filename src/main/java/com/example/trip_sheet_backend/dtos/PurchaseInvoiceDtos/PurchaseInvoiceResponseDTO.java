package com.example.trip_sheet_backend.dtos.PurchaseInvoiceDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchaseInvoice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PurchaseInvoiceResponseDTO {
  private UUID id;
  private String invoiceNumber;
  private UUID tripSummaryId;
  private BigDecimal amountPayable;
  private BigDecimal amountReceivable;
  private BigDecimal earning;
  private String currencyCode;
  private String rateCardPackageName;
  private String notes;
  private PurchaseInvoice.PurchaseInvoiceStatus status;
  private VendorReference payerVendor;
  private VendorReference payeeVendor;

  // PO-compatible calculated fields.
  private BigDecimal baseFareAmount, baseFareQty, baseFareTotal;
  private BigDecimal extraKmChargeAmount, extraKmQty, extraKmTotal;
  private BigDecimal extraHrChargeAmount, extraHrQty, extraHrTotal;
  private BigDecimal dailyAllowanceChargeAmount, dailyAllowanceQty, dailyAllowanceTotal;
  private BigDecimal earlyAllowanceChargeAmount, earlyAllowanceQty, earlyAllowanceTotal;
  private BigDecimal lateAllowanceChargeAmount, lateAllowanceQty, lateAllowanceTotal;
  private BigDecimal hourlyAllowanceCharge, hourlyAllowanceQty, hourlyAllowanceAmount;
  private BigDecimal tollChargeAmount, tollQty, tollTotal;
  private BigDecimal parkingChargeAmount, parkingQty, parkingTotal;
  private BigDecimal otherChargeAmount, otherQty, otherTotal;
  private BigDecimal taxableSubTotal, gstPercentage, gstAmount;
  private BigDecimal cgstPercentage, cgstAmount, sgstPercentage, sgstAmount;
  private BigDecimal igstPercentage, igstAmount;
  private BigDecimal taxableTotalWithGst, nonTaxableTotal, roundOffAmount, totalAmount;

  public PurchaseInvoiceResponseDTO(UUID id, String invoiceNumber, UUID tripSummaryId, BigDecimal amountPayable,
      BigDecimal amountReceivable, BigDecimal earning,
      String currencyCode, String rateCardPackageName, String notes,
      PurchaseInvoice.PurchaseInvoiceStatus status, VendorReference payerVendor, VendorReference payeeVendor) {
    this.id = id;
    this.invoiceNumber = invoiceNumber;
    this.tripSummaryId = tripSummaryId;
    this.amountPayable = amountPayable;
    this.amountReceivable = amountReceivable;
    this.earning = earning;
    this.currencyCode = currencyCode;
    this.rateCardPackageName = rateCardPackageName;
    this.notes = notes;
    this.status = status;
    this.payerVendor = payerVendor;
    this.payeeVendor = payeeVendor;
  }

  @Getter @AllArgsConstructor
  public static class VendorReference {
    private UUID id;
    private String name;
  }

  public static PurchaseInvoiceResponseDTO fromEntity(PurchaseInvoice value) {
    PurchaseInvoiceResponseDTO response = new PurchaseInvoiceResponseDTO(value.getId(), value.getInvoiceNumber(),
        value.getTripSummary() == null ? null : value.getTripSummary().getId(),
        value.getAmountPayable(), value.getAmountReceivable(), value.getEarning(),
        value.getCurrencyCode(), value.getRateCardPackageName(), value.getNotes(), value.getStatus(),
        vendor(value.getPayerVendor()), vendor(value.getPayeeVendor()));
    response.baseFareAmount = value.getBaseFareAmount(); response.baseFareQty = value.getBaseFareQty(); response.baseFareTotal = value.getBaseFareTotal();
    response.extraKmChargeAmount = value.getExtraKmChargeAmount(); response.extraKmQty = value.getExtraKmQty(); response.extraKmTotal = value.getExtraKmTotal();
    response.extraHrChargeAmount = value.getExtraHrChargeAmount(); response.extraHrQty = value.getExtraHrQty(); response.extraHrTotal = value.getExtraHrTotal();
    response.dailyAllowanceChargeAmount = value.getDailyAllowanceChargeAmount(); response.dailyAllowanceQty = value.getDailyAllowanceQty(); response.dailyAllowanceTotal = value.getDailyAllowanceTotal();
    response.earlyAllowanceChargeAmount = value.getEarlyAllowanceChargeAmount(); response.earlyAllowanceQty = value.getEarlyAllowanceQty(); response.earlyAllowanceTotal = value.getEarlyAllowanceTotal();
    response.lateAllowanceChargeAmount = value.getLateAllowanceChargeAmount(); response.lateAllowanceQty = value.getLateAllowanceQty(); response.lateAllowanceTotal = value.getLateAllowanceTotal();
    response.hourlyAllowanceCharge = value.getHourlyAllowanceCharge(); response.hourlyAllowanceQty = value.getHourlyAllowanceQty(); response.hourlyAllowanceAmount = value.getHourlyAllowanceAmount();
    response.tollChargeAmount = value.getTollChargeAmount(); response.tollQty = value.getTollQty(); response.tollTotal = value.getTollTotal();
    response.parkingChargeAmount = value.getParkingChargeAmount(); response.parkingQty = value.getParkingQty(); response.parkingTotal = value.getParkingTotal();
    response.otherChargeAmount = value.getOtherChargeAmount(); response.otherQty = value.getOtherQty(); response.otherTotal = value.getOtherTotal();
    response.taxableSubTotal = value.getTaxableSubTotal(); response.gstPercentage = value.getGstPercentage(); response.gstAmount = value.getGstAmount();
    response.cgstPercentage = value.getCgstPercentage(); response.cgstAmount = value.getCgstAmount(); response.sgstPercentage = value.getSgstPercentage(); response.sgstAmount = value.getSgstAmount();
    response.igstPercentage = value.getIgstPercentage(); response.igstAmount = value.getIgstAmount();
    response.taxableTotalWithGst = value.getTaxableTotalWithGst(); response.nonTaxableTotal = value.getNonTaxableTotal(); response.roundOffAmount = value.getRoundOffAmount(); response.totalAmount = value.getTotalAmount();
    return response;
  }

  private static VendorReference vendor(com.example.trip_sheet_backend.models.Tenant tenant) {
    return tenant == null ? null : new VendorReference(tenant.getId(), tenant.getTenantName());
  }

}

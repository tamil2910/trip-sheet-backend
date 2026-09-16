package com.example.trip_sheet_backend.dtos.PurchaseInvoiceDtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.trip_sheet_backend.models.PurchaseInvoice;
import com.example.trip_sheet_backend.models.Invoice;
import com.example.trip_sheet_backend.models.PurchaseOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PurchaseInvoiceResponseDTO {
  private UUID id;
  private String orderNumber;
  private String invoiceNumber;
  private String purchaseInvoiceNumber;
  private UUID purchaseOrderId;
  private Boolean isSourceInvoice;
  private UUID tripSummaryId;
  private BigDecimal amountPayable;
  private BigDecimal creditDebitNoteAppliedAmount;
  private BigDecimal currentPayableAmount;
  private BigDecimal amountReceivable;
  private BigDecimal earning;
  private String currencyCode;
  private String rateCardPackageName;
  private String notes;
  private Long tripStartKmOdo;
  private Long tripEndKmOdo;
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

  public PurchaseInvoiceResponseDTO(UUID id, String orderNumber, String invoiceNumber, String purchaseInvoiceNumber,
      UUID purchaseOrderId, UUID tripSummaryId, BigDecimal amountPayable,
      BigDecimal amountReceivable, BigDecimal earning,
      String currencyCode, String rateCardPackageName, String notes,
      PurchaseInvoice.PurchaseInvoiceStatus status, VendorReference payerVendor, VendorReference payeeVendor) {
    this.id = id;
    this.orderNumber = orderNumber;
    this.invoiceNumber = invoiceNumber;
    this.purchaseInvoiceNumber = purchaseInvoiceNumber;
    this.purchaseOrderId = purchaseOrderId;
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
    PurchaseInvoiceResponseDTO response = new PurchaseInvoiceResponseDTO(value.getId(), value.getOrderNumber(), value.getInvoiceNumber(),
        value.getPurchaseInvoiceNumber(), value.getSourceInvoice() == null || value.getSourceInvoice().getPurchaseOrder() == null ? null : value.getSourceInvoice().getPurchaseOrder().getId(),
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
    response.tripStartKmOdo = value.getTripStartKmOdo();
    response.tripEndKmOdo = value.getTripEndKmOdo();
    response.creditDebitNoteAppliedAmount = value.getCreditDebitNoteAppliedAmount();
    response.currentPayableAmount = value.getCurrentPayableAmount();
    return response;
  }

  /** View of Vendor B's invoice awaiting Vendor A's approval. It is not yet a PurchaseInvoice row. */
  public static PurchaseInvoiceResponseDTO fromSourceInvoice(Invoice invoice) {
    PurchaseOrder order = invoice.getPurchaseOrder();
    PurchaseInvoiceResponseDTO response = new PurchaseInvoiceResponseDTO(invoice.getId(), order.getOrderNumber(), invoice.getInvoiceNumber(),
        null, order.getId(), order.getTripSummary() == null ? null : order.getTripSummary().getId(),
        order.getTotalAmount(), null, null, order.getCurrencyCode(), order.getRateCardPackageName(), order.getNotes(),
        PurchaseInvoice.PurchaseInvoiceStatus.GENERATED, vendor(order.getTenant()), vendor(order.getSupplierVendor()));
    response.isSourceInvoice = true;
    response.totalAmount = order.getTotalAmount();
    response.tripStartKmOdo = order.getTripStartKmOdo();
    response.tripEndKmOdo = order.getTripEndKmOdo();
    response.baseFareAmount = order.getBaseFareAmount(); response.baseFareQty = order.getBaseFareQty(); response.baseFareTotal = order.getBaseFareTotal();
    response.extraKmChargeAmount = order.getExtraKmChargeAmount(); response.extraKmQty = order.getExtraKmQty(); response.extraKmTotal = order.getExtraKmTotal();
    response.extraHrChargeAmount = order.getExtraHrChargeAmount(); response.extraHrQty = order.getExtraHrQty(); response.extraHrTotal = order.getExtraHrTotal();
    response.dailyAllowanceChargeAmount = order.getDailyAllowanceChargeAmount(); response.dailyAllowanceQty = order.getDailyAllowanceQty(); response.dailyAllowanceTotal = order.getDailyAllowanceTotal();
    response.earlyAllowanceChargeAmount = order.getEarlyAllowanceChargeAmount(); response.earlyAllowanceQty = order.getEarlyAllowanceQty(); response.earlyAllowanceTotal = order.getEarlyAllowanceTotal();
    response.lateAllowanceChargeAmount = order.getLateAllowanceChargeAmount(); response.lateAllowanceQty = order.getLateAllowanceQty(); response.lateAllowanceTotal = order.getLateAllowanceTotal();
    response.hourlyAllowanceCharge = order.getHourlyAllowanceCharge(); response.hourlyAllowanceQty = order.getHourlyAllowanceQty(); response.hourlyAllowanceAmount = order.getHourlyAllowanceAmount();
    response.tollChargeAmount = order.getTollChargeAmount(); response.tollQty = order.getTollQty(); response.tollTotal = order.getTollTotal();
    response.parkingChargeAmount = order.getParkingChargeAmount(); response.parkingQty = order.getParkingQty(); response.parkingTotal = order.getParkingTotal();
    response.otherChargeAmount = order.getOtherChargeAmount(); response.otherQty = order.getOtherQty(); response.otherTotal = order.getOtherTotal();
    response.taxableSubTotal = order.getTaxableSubTotal(); response.gstPercentage = order.getGstPercentage(); response.gstAmount = order.getGstAmount();
    response.cgstPercentage = order.getCgstPercentage(); response.cgstAmount = order.getCgstAmount(); response.sgstPercentage = order.getSgstPercentage(); response.sgstAmount = order.getSgstAmount();
    response.igstPercentage = order.getIgstPercentage(); response.igstAmount = order.getIgstAmount();
    response.taxableTotalWithGst = order.getTaxableTotalWithGst(); response.nonTaxableTotal = order.getNonTaxableTotal(); response.roundOffAmount = order.getRoundOffAmount();
    return response;
  }

  private static VendorReference vendor(com.example.trip_sheet_backend.models.Tenant tenant) {
    return tenant == null ? null : new VendorReference(tenant.getId(), tenant.getTenantName());
  }

}

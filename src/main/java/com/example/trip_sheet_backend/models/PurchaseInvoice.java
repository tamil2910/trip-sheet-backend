package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A private payable created for one delegation hop.  It deliberately contains
 * no organisation details and is visible only to its payer and payee vendors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_invoices", uniqueConstraints = @UniqueConstraint(columnNames = "delegation_history_id"))
public class PurchaseInvoice extends BaseModel implements TenantScoped {

  /** Kept null until the receiving vendor explicitly raises/finalises an invoice. */
  private String invoiceNumber;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delegation_history_id", nullable = false, unique = true)
  private VendorDelegationHistory delegationHistory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_summary_id", nullable = false)
  private TripSummary tripSummary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payer_vendor_id", nullable = false)
  private Tenant payerVendor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payee_vendor_id", nullable = false)
  private Tenant payeeVendor;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amountPayable;

  /** What the payer earns from its immediately preceding relationship. */
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amountReceivable;

  /** amountReceivable - amountPayable, retained for the payer's margin report. */
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal earning;

  private String currencyCode;
  private String rateCardPackageName;
  private String notes;

  @Enumerated(EnumType.STRING)
  private PurchaseInvoiceStatus status = PurchaseInvoiceStatus.GENERATED;

  // Immutable trip/party and pricing snapshot (kept in parity with PurchaseOrder).
  private String orderNumber;
  private String documentType;
  private Long documentDate;
  private Long dueDate;
  private Long billingPeriodStart;
  private Long billingPeriodEnd;
  private String billToName;
  private String billToCode;
  private String billToGstin;
  private String billToAddress;
  private String supplierName;
  private String supplierPhone;
  private String supplierAddress;
  private Integer lineItemCount;
  @jakarta.persistence.Lob
  @Column(columnDefinition = "LONGTEXT")
  private String lineItemsSnapshot;
  private Long garageStartTime;
  private Long garageEndTime;
  private Long tripStartTime;
  private Long tripStartKmOdo;
  private Long tripStartKmOdoImage;
  private Long tripEndTime;
  private Long tripEndKmOdo;
  private Long tripEndKmOdoImage;
  private Long tripDuration;
  private Long tripDistance;
  private Long tripExtraKmOdo;
  private Long tripExtraKm;
  private Long tripExtraHr;

  @Column(name = "trip_startgpskm")
  private Long tripStartGPSKM;

  @Column(name = "trip_endgpskm")
  private Long tripEndGPSKM;

  @Column(name = "trip_gps_duration")
  private Long tripGPSDuration;

  @Column(name = "trip_gps_distance")
  private Long tripGPSDistance;
  
  private Double dispatchLat;
  private Double dispatchLng;
  private Double arrivedLat;
  private Double arrivedLng;
  private Double tripStartLat;
  private Double tripStartLng;
  private Double tripEndLat;
  private Double tripEndLng;
  private Double garageEndLat;
  private Double garageEndLng;
  private String tripCalculationFieldName;
  private String extraHrCalculationFieldName;
  private String extraKmCalculationFieldName;
  private BigDecimal baseFareAmount;
  private BigDecimal baseFareQty;
  private BigDecimal baseFareTotal;
  private BigDecimal extraKmChargeAmount;
  private BigDecimal extraKmQty;
  private BigDecimal extraKmTotal;
  private BigDecimal extraHrChargeAmount;
  private BigDecimal extraHrQty;
  private BigDecimal extraHrTotal;
  private BigDecimal dailyAllowanceChargeAmount;
  private BigDecimal dailyAllowanceQty;
  private BigDecimal dailyAllowanceTotal;
  private BigDecimal earlyAllowanceChargeAmount;
  private BigDecimal earlyAllowanceQty;
  private BigDecimal earlyAllowanceTotal;
  private BigDecimal lateAllowanceChargeAmount;
  private BigDecimal lateAllowanceQty;
  private BigDecimal lateAllowanceTotal;
  private BigDecimal hourlyAllowanceCharge;
  private BigDecimal hourlyAllowanceQty;
  private BigDecimal hourlyAllowanceAmount;
  private BigDecimal tollChargeAmount;
  private BigDecimal tollQty;
  private BigDecimal tollTotal;
  private BigDecimal parkingChargeAmount;
  private BigDecimal parkingQty;
  private BigDecimal parkingTotal;
  private BigDecimal otherChargeAmount;
  private BigDecimal otherQty;
  private BigDecimal otherTotal;
  private BigDecimal taxableSubTotal;
  private BigDecimal gstPercentage;
  private BigDecimal gstAmount;
  private BigDecimal cgstPercentage;
  private BigDecimal cgstAmount;
  private BigDecimal sgstPercentage;
  private BigDecimal sgstAmount;
  private BigDecimal igstPercentage;
  private BigDecimal igstAmount;
  private BigDecimal taxableTotalWithGst;
  private BigDecimal nonTaxableTotal;
  private BigDecimal roundOffAmount;
  private BigDecimal totalAmount;

  public enum PurchaseInvoiceStatus {
    GENERATED, PAYMENT_RECEIVED, CANCELLED
  }

  @Override
  public Tenant getTenant() {
    return payerVendor;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.payerVendor = tenant;
  }
}

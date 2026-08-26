package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseModel implements TenantScoped {

  private String orderNumber;
  private String documentType;
  private String currencyCode;

  // Header fields found in the supplied purchase-order PDF.
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

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String lineItemsSnapshot;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_summary_id")
  private TripSummary tripSummary;

  /** Set on source POs when they are included in a reversible combined PO. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "combined_purchase_order_id")
  private PurchaseOrder combinedPurchaseOrder;

  @Enumerated(EnumType.STRING)
  private PurchaseOrderStatus status;

  @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PurchaseOrderAllocation> allocations = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

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

  private Long tripStartGPSKM;
  private Long tripEndGPSKM;

  private Long tripGPSDuration;
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

  private String rateCardPackageName;

  // Taxable package pricing from vendor rate cards: baseFare * quantity = amount
  private BigDecimal baseFareAmount;
  private BigDecimal baseFareQty;
  private BigDecimal baseFareTotal;

  // Taxable add-on pricing from vendor rate cards
  private BigDecimal extraKmChargeAmount;
  private BigDecimal extraKmQty;
  private BigDecimal extraKmTotal;

  private BigDecimal extraHrChargeAmount;
  private BigDecimal extraHrQty;
  private BigDecimal extraHrTotal;

  // Taxable allowance pricing from the vendor organisation rate card
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

  // Taxable reimbursable trip charges
  private BigDecimal tollChargeAmount;
  private BigDecimal tollQty;
  private BigDecimal tollTotal;

  private BigDecimal parkingChargeAmount;
  private BigDecimal parkingQty;
  private BigDecimal parkingTotal;

  private BigDecimal otherChargeAmount;
  private BigDecimal otherQty;
  private BigDecimal otherTotal;

  // Totals
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
  private String notes;

  public enum PurchaseOrderStatus {
    GENERATED,
    VERIFIED,
    REJECTED,
    INVOICED
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}

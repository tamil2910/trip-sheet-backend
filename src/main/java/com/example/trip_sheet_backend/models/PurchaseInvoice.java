package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @Override
  public Tenant getTenant() {
    return payerVendor;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.payerVendor = tenant;
  }
}

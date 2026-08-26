package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vendor-specific configuration and counter for generated purchase-order numbers. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_order_number_rules", indexes = {
    @Index(name = "idx_po_number_rule_vendor", columnList = "vendor_id"),
    @Index(name = "idx_po_number_rule_vendor_default", columnList = "vendor_id,is_default")
})
public class PurchaseOrderNumberRule extends BaseModel {

  /** Either a calendar year (2026) or a financial year (2026_2027). */
  @NotBlank
  @Column(nullable = false, length = 9)
  private String period;

  /** Optional series suffix, for example A. */
  private String suffix;

  @NotNull
  @Positive
  @Column(nullable = false)
  private Long sequenceStart;

  /** Persisted counter; this is incremented when a PO is generated. */
  @NotNull
  @Positive
  @Column(nullable = false)
  private Long nextSequence;

  /** Separate counter for COMBINED_PO numbers. */
  @NotNull
  @Positive
  @Column(nullable = false)
  private Long nextCombinedSequence;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Tenant vendor;

  public String formatPurchaseOrderNumber(long sequence) {
    return formatNumber("PO", sequence);
  }

  public String formatCombinedPurchaseOrderNumber(long sequence) {
    return formatNumber("COMBINED_PO", sequence);
  }

  private String formatNumber(String prefix, long sequence) {
    // The standard series starts as 01, while higher configured starts (such as
    // 125) retain their natural width.
    int width = Math.max(2, String.valueOf(sequenceStart).length());
    String serial = String.format("%0" + width + "d", sequence);
    return suffix == null || suffix.isBlank()
        ? prefix + "-" + period + "-" + serial
        : prefix + "-" + period + "-" + suffix + "-" + serial;
  }
}

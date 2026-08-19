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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_number_rules", indexes = {
    @Index(name = "idx_invoice_number_rule_tenant", columnList = "tenant_id"),
    @Index(name = "idx_invoice_number_rule_tenant_default", columnList = "tenant_id,is_default")
})
public class InvoiceNumberRule extends BaseModel implements TenantScoped {

  @NotBlank(message = "prefix is required")
  @Column(nullable = false)
  private String prefix;

  /** Automatically calculated from the creation date, e.g. 2026_2027. */
  @NotBlank(message = "financialYear is required")
  @Column(nullable = false, length = 9)
  private String financialYear;

  @Column(nullable = true)
  private String suffix;

  /** First number in the invoice series for this rule. */
  @NotNull(message = "sequenceStart is required")
  @Positive(message = "sequenceStart must be greater than zero")
  @Column(nullable = false)
  private Long sequenceStart;

  /** The next serial number to allocate when invoice generation is introduced. */
  @NotNull
  @Positive
  @Column(nullable = false)
  private Long nextSequence;

  @Column(name = "is_default", nullable = false)
  private Boolean isDefault = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  /** Formats a number in the configured invoice series, preserving the start-number width. */
  public String formatInvoiceNumber(long sequence) {
    int width = String.valueOf(sequenceStart).length();
    String serial = String.format("%0" + width + "d", sequence);
    return suffix == null || suffix.isBlank()
        ? prefix + "-" + financialYear + "-" + serial
        : prefix + "-" + financialYear + "-" + suffix + "-" + serial;
  }
}

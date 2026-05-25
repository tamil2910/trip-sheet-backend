package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trip_billing_rules", indexes = { @Index(columnList = "tenant_id") })
public class TripBillingRule extends BaseModel implements TenantScoped {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  @Enumerated(EnumType.STRING)
  private BillingBasis billingBasis;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_field_id")
  private CustomField costCenterCustomField;

  @Enumerated(EnumType.STRING)
  private InvoiceGrouping invoiceGrouping;

  private Boolean active = true;

  public enum BillingBasis {
    TRIP_WISE,
    CUSTOM_FIELD_WISE
  }

  public enum InvoiceGrouping {
    TRIP,
    PO,
    // CONSOLIDATED_PO,
    // INDIVIDUAL_PO
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}

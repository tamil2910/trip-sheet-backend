package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trip_billing_allocations", indexes = { @Index(columnList = "trip_id"), @Index(columnList = "tenant_id") })
public class TripBillingAllocation extends BaseModel implements TenantScoped {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip trip;

  @Enumerated(EnumType.STRING)
  private AllocationType allocationType;

  // e.g. cost-center code or passenger id as string
  private String allocationKey;

  @Column(precision = 10, scale = 2)
  private BigDecimal sharePercent;

  @Column(precision = 19, scale = 4)
  private BigDecimal shareAmount;

  @Enumerated(EnumType.STRING)
  private AllocationStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  public enum AllocationType {
    TRIP_WISE,
    CUSTOM_FIELD_WISE,
    PASSENGER_WISE
  }

  public enum AllocationStatus {
    GENERATED,
    VERIFIED,
    APPROVED,
    REJECTED
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}

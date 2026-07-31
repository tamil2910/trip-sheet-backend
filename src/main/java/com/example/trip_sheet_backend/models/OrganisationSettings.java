package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "organisation_settings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id"}),
    indexes = { @Index(columnList = "tenant_id") }
)
public class OrganisationSettings extends BaseModel implements TenantScoped {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(nullable = false)
  private Boolean autoPoGenerationEnabled = false;

  @Column(nullable = false)
  private Boolean allowChildBookingAttachOnClosedTrip = false;

  @Column(nullable = false)
  private Boolean employeeKmRestrictionEnabled = false;

  private Integer employeeBookingKmLimit;

  @Column(nullable = false)
  private Boolean autoAllotEnabled = false;

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}

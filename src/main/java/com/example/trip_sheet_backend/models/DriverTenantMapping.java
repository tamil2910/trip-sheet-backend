package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
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
    name = "driver_tenant_mappings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"driver_id", "tenant_id"})
    }
)
public class DriverTenantMapping extends BaseModel implements TenantScoped {

  @NotNull(message = "Driver is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @NotNull(message = "Tenant is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  private Boolean active = true;

  private Long linkedAt;

  @Override
  public Tenant getTenant() {
    return tenant;
  }

  @Override
  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }
}

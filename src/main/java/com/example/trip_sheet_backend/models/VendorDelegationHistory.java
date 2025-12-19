package com.example.trip_sheet_backend.models;

import java.time.LocalDateTime;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_vendor_delegation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorDelegationHistory extends BaseModel implements TenantScoped {
  
  @ManyToOne
  private Trip trip;

  @ManyToOne
  private Tenant fromVendor;

  @ManyToOne
  private Tenant toVendor;

  private LocalDateTime delegatedAt;

  @Override
  public Tenant getTenant() {
      return trip != null ? trip.getTenant() : null;
  }

  @Override
  public void setTenant(Tenant tenant) {
    setTenant(tenant);
  }
}

package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "trip_charges")
public class TripCharges extends BaseModel implements TenantScoped {
  @Valid
  @NotNull(message = "Trip id is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip tripId;

  @Enumerated(EnumType.STRING)
  private chargeType type;

  public enum chargeType {
    Toll, Parking, Other
  }

  private String receipt;
  private Long amount;
  private String description;


  @Valid
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  public void setTenant(Tenant tenant) {
    this.tenant = tenant;
  }

  @Override
  public Tenant getTenant() {
    return tenant;
  }

}


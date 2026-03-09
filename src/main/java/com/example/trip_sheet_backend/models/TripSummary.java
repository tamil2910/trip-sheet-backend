package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "trip_summaries")
public class TripSummary extends BaseModel implements TenantScoped {
  @Valid
  @NotNull(message = "Trip id is required")
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip tripId;

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

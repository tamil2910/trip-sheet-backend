package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_service_item_categories", uniqueConstraints = @UniqueConstraint(
    name = "uq_vehicle_service_item_category_tenant_normalized_name",
    columnNames = {"tenant_id", "normalized_name"}))
@Getter
@Setter
@NoArgsConstructor
public class VehicleServiceItemCategory extends BaseModel implements TenantScoped {
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(name = "normalized_name", nullable = false, length = 180)
  private String normalizedName;
}

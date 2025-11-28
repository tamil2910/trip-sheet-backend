package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "vehicle_driver_mappings")
public class VehicleDriverMapping extends BaseModel {

  @NotNull(message = "Vehicle is required to link with driver")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  @JoinColumn(name = "vehicle_id")
  private Vehicle vehicle;

  @NotNull(message = "Driver is required to link with driver")
  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  @JoinColumn(name = "driver_id")
  private Driver driver;

  @NotNull(message = "Tenant is required for vehicle-driver mapping")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @NotNull(message = "Active/ Inactive is required for mapping driver-vehicle")
  private typeStatus status; 

  public enum typeStatus {
    ACTIVE, INACTIVE
  }
}

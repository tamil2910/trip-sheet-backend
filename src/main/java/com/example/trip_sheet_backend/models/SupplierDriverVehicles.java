package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "vendor_partner_driver_vehicles", uniqueConstraints = {
  @UniqueConstraint(name = "uq_vpdv_primary_partner_phone", columnNames = {
    "primary_vendor_id", "partner_vendor_id", "phone"
  }),
  @UniqueConstraint(name = "uq_vpdv_primary_partner_email", columnNames = {
    "primary_vendor_id", "partner_vendor_id", "email"
  }),
  @UniqueConstraint(name = "uq_vpdv_primary_partner_vehicle_number", columnNames = {
    "primary_vendor_id", "partner_vendor_id", "vehicle_number"
  })
})
public class SupplierDriverVehicles extends BaseModel {

  @NotNull(message = "Primary vendor is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "primary_vendor_id", nullable = false)
  private Tenant primaryVendor;

  @NotNull(message = "Partner vendor is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "partner_vendor_id", nullable = false)
  private Tenant partnerVendor;

  @NotNull(message = "Type is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type;

  @Column(name = "driver_name")
  private String driverName;

  @Column(name = "phone")
  private String phone;

  @Column(name = "email")
  private String email;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "driver_id")
  private Driver driver;

  @Column(name = "vehicle_number")
  private String vehicleNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id")
  private VehicleType vehicleType;

  public enum Type {
    DRIVER,
    VEHICLE
  }
}
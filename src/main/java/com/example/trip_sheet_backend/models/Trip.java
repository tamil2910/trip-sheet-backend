package com.example.trip_sheet_backend.models;
  
import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
// import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trips")
public class Trip extends BaseModel implements TenantScoped {

  @Enumerated(EnumType.STRING)
  private TripStatus tripStatus;

  @JsonIgnore
  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  private Booking booking;  // parent booking (optional)

  // Delegation
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_trip_id")
  private Trip parentTrip;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_id")
  private Tenant vendor; // executing vendor

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_by_vendor_id")
  private Tenant assignedByVendor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_vendor_id")
  private Tenant previousVendor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organisation_id", nullable = false)
  private Tenant organisation; // the corporate owning the trip

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant; // Tenant context (taken from token) Could be vendor OR corporate

  private String notes;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "driver_id")
  private Driver driver;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id")
  private Vehicle vehicle;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "duty_type_id")
  private DutyType dutyType;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id")
  private VehicleType vehicleType;

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TripStop> stops = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "trip_people_tenants",
    joinColumns = @JoinColumn(name = "trip_id"),
    inverseJoinColumns = @JoinColumn(name = "people_tenant_id")
  )
  private List<PeopleTenant> passengers = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booker_id")
  private PeopleTenant booker;

  public enum TripStatus {
      CREATED, REQUESTING, CONFIRMED, ALLOTTED,
      DISPATCHED, ARRIVED, STARTED, COMPLETED,
      CLOSED, CANCELLED, FAILED, NO_SHOW, EXPIRED
  }

  private Long pickupTime;
  private Long endDate;


  @Override
  public Tenant getTenant() {
      return tenant;
  }
}

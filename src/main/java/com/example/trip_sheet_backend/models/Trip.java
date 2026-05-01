package com.example.trip_sheet_backend.models;
  
import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;
// import com.fasterxml.jackson.annotation.JsonBackReference;
// import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.Index;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "trips",
  indexes = {
    @Index(name = "idx_trip_code", columnList = "trip_code"),
    @Index(name = "idx_trip_status", columnList = "trip_status"),
    @Index(name = "idx_trip_tenant_deleted_pickup", columnList = "tenant_id, is_deleted, pickup_time"),
    @Index(name = "idx_trip_tenant_deleted_start_end", columnList = "tenant_id, is_deleted, start_date, end_date"),
    @Index(name = "idx_vendor_id", columnList = "vendor_id"),
    @Index(name = "idx_organisation_id", columnList = "organisation_id"),
    @Index(name = "idx_driver_id", columnList = "driver_id"),
    @Index(name = "idx_vehicle_id", columnList = "vehicle_id"),
    @Index(name = "idx_dispatch_center_id", columnList = "dispatch_center_id"),
    @Index(name = "idx_duty_type_id", columnList = "duty_type_id"),
    @Index(name = "idx_vehicle_type_id", columnList = "vehicle_type_id")
  }
)
@BatchSize(size = 50)
public class Trip extends BaseModel implements TenantScoped {

  @Column(name = "trip_code")
  private String tripCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "trip_status")
  private TripStatus tripStatus;

  @Enumerated(EnumType.STRING)
  private TripType tripType; // SINGLE, MULTI_DAY, RECURRING

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_center_id")
  private DispatchCenter dispatchCenter;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "duty_type_id")
  private DutyType dutyType;

  // @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id")
  private VehicleType vehicleType;

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TripStop> stops = new ArrayList<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "trip_people_tenants",
    joinColumns = @JoinColumn(name = "trip_id"),
    inverseJoinColumns = @JoinColumn(name = "people_tenant_id")
  )
  @BatchSize(size = 50)
  private List<PeopleTenant> passengers = new ArrayList<>();

  @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TripPassengerCustomFieldValue> passengerCustomFieldValues = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booker_id")
  private PeopleTenant booker;

  public enum TripStatus {
      CREATED, REQUESTING, CONFIRMED, ALLOTTED,
      DISPATCHED, ARRIVED, STARTED, COMPLETED,
      CLOSED, CANCELLED, FAILED, NO_SHOW, EXPIRED
  }

  public enum TripType {
    SINGLE,
    MULTI_DAY,
    RECURRING
  }

  public enum RecurrenceFrequency {
      WEEKLY,
      MONTHLY
  }

  private Integer recurrenceInterval;

  private String daysOfWeek; // MON,WED,FRI for WEEKLY recurrence

  @Enumerated(EnumType.STRING)
  private RecurrenceFrequency recurrenceFrequency;

  @Column(columnDefinition = "BIGINT")
  private Long pickupTime;

  @Column(columnDefinition = "BIGINT")
  private Long startDate;
  @Column(columnDefinition = "BIGINT")
  private Long endDate;

  @Column(columnDefinition = "BIGINT")
  private Long startOtp;
  @Column(columnDefinition = "BIGINT")
  private Long endOtp;

  private Boolean isManualTrip = false;

  @Override
  public Tenant getTenant() {
      return tenant;
  }
}

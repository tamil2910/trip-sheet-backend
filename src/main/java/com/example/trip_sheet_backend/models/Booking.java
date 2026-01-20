package com.example.trip_sheet_backend.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseModel implements TenantScoped {
  private String bookingCode;

  private LocalDate startDate;
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  private BookingType bookingType; // SINGLE, MULTI_DAY, RECURRING

  private Boolean autoGenerateTrips;

   // Booking is created by Vendor mostly
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Tenant vendor; 

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id")
  private Tenant tenant; // Tenant context (taken from token) Could be vendor OR corporate

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Trip> trips = new ArrayList<>();

  public enum BookingType {
      SINGLE,
      MULTI_DAY,
      RECURRING
  }

  @Override
  public Tenant getTenant() {
      return tenant; 
  }
}

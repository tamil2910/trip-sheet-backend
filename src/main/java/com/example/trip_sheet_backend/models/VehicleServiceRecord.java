package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.math.BigDecimal;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_services")
@Getter @Setter @NoArgsConstructor
public class VehicleServiceRecord extends BaseModel implements TenantScoped {
  public enum ServiceType {
    PERIODIC_SERVICE,
    GENERAL_RUNNING_REPAIR,
    BREAKDOWN,
    TYRE_WHEEL,
    BODY_SHOP_DENTER,
    BATTERY,
    AC_SERVICE,
    ELECTRICAL,
    INSURANCE_CLAIM,
    INSPECTION,
    DEEP_CLEAN
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Enumerated(EnumType.STRING)
  @Column(name = "service_type", nullable = false, length = 40)
  private ServiceType serviceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "authorized_service_center_id")
  private AuthorizedServiceCenter authorizedServiceCenter;

  @Column(name = "center_name", length = 180)
  private String centerName;

  @Column(name = "calling_person_name", length = 160)
  private String callingPersonName;

  @Column(name = "phone_number", length = 40)
  private String phoneNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_center_id")
  private DispatchCenter dispatchCenter;

  @Column(name = "request_date", nullable = false)
  private Long requestDate;

  @Column(name = "estimated_completion_date")
  private Long estimatedCompletionDate;

  @Column(name = "taken_in_for", nullable = false, length = 500)
  private String takenInFor;

  @Column(name = "odometer_km", nullable = false)
  private Integer odometerKm;

  @Column(name = "request_notes", columnDefinition = "TEXT")
  private String requestNotes;

  @Column(name = "estimated_amount", precision = 12, scale = 2)
  private BigDecimal estimatedAmount;

  @Column(name = "paid_amount", precision = 12, scale = 2)
  private BigDecimal paidAmount;
}

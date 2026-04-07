package com.example.trip_sheet_backend.models;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_partner_rate_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerRateCard extends BaseModel {

  @ManyToOne
  @JoinColumn(name = "primary_vendor_id", nullable = false)
  private Tenant primaryVendor;

  @JsonBackReference
  @ManyToOne
  @JoinColumn(name = "vendor_partner_id", nullable = false)
  private VendorPartner vendorPartner;

  @ManyToOne
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @ManyToOne
  @JoinColumn(name = "duty_type_id", nullable = false)
  private DutyType dutyType;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal baseFare;

  @Column(precision = 12, scale = 2)
  private BigDecimal extraKmCharges;

  @Column(precision = 12, scale = 2)
  private BigDecimal extraHrCharges;

  @Column(precision = 12, scale = 2)
  private BigDecimal dailyAllowanceCharges;

  @Column(precision = 12, scale = 2)
  private BigDecimal earlyAllowanceCharges;

  @Column(precision = 12, scale = 2)
  private BigDecimal lateAllowanceCharges;

  private Integer switchCutOffHrs;

  private Integer switchCutOffKms;

  @ManyToOne
  @JoinColumn(name = "switch_duty_type_id")
  private DutyType switchDutyType;

  @Column(precision = 12, scale = 2)
  private BigDecimal hourlyAllowance;

  @ManyToOne
  @JoinColumn(name = "no_show_duty_type_id")
  private DutyType noShowDutyType;

  private Integer noOfDaysHourCutoff;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime earlyAllowanceStartTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime lateAllowanceStartTime;

  private Integer allowanceCutOffHrs;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

  private Long approvedAt;

  private String approvedBy;

  public enum ApprovalStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
  }
}

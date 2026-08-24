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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_organisation_rate_cards",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { 
      "vendor_organisation_id",
      "vehicle_type_id",
      "duty_type_id",
      "city"
    }
  )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationRateCard extends BaseModel {

  @ManyToOne
  @JoinColumn(name = "vendor_id", nullable = false)
  private Tenant vendor;

  @JsonBackReference
  @ManyToOne
  @JoinColumn(name = "vendor_organisation_id", nullable = false)
  private VendorOrganisation vendorOrganisation;

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

  
  private Integer switchCutOffHrs; // If hour exceeds the selected package hour, it has to jump to another duty type package (eg:  4hr40km becomes 6hr60km)
  
  private Integer switchCutOffKms; // If km exceeds the selected package km, it has to jump to another duty type package (eg:  4hr40km becomes 6hr60km)
  
  @ManyToOne
  @JoinColumn(name = "switch_duty_type_id")
  private DutyType switchDutyType; // switching duty type based on switchCutOffHrs and switchCutOffKms
  
  @ManyToOne
  @JoinColumn(name = "no_show_duty_type_id")
  private DutyType noShowDutyType;// if last minute trip cancelled by client or vehicle is dispatched then they decide not required this kind of vehicle or anyother situation if trip is marked as isNoShow true then what ever in noShowDutyType of duty_type_id will be taken to  bill it, (eg: 6hr60km package is taken by clent, but client is cancelling their plan then requesting for minimal charge, if it is the case if you purAirport transfer or 4hrs40km as noShowDutyType then that trip will be billed for Airport Transfer package or $hr$0km package which is selected)
  
  @Column(precision = 12, scale = 2)
  private BigDecimal dailyAllowanceCharges; // Only for outstation trips

  @Column(precision = 12, scale = 2)
  private BigDecimal earlyAllowanceCharges; // 

  @Column(precision = 12, scale = 2)
  private BigDecimal lateAllowanceCharges; // 


  private Boolean isHourlyAllowance; // if true then hourlyAllowance will be applied for lateAllowanceStartTime to allowanceCutOffHrs, if false then lateAllowanceCharges will be applied for lateAllowanceStartTime to allowanceCutOffHrs

  private Integer allowanceCutOffHrs; // it is applied for late night duties, here number of hours will be added, if lateAllowanceStartTime is 10PM and allowanceCutOffHrs is 4 means till 10PM to 02AM for these 4hrs will be consider as hourlyAllowance/lateAllowanceCharges if isHourlyAllowance true or else will be considered are lateAllowanceCharges. if isHourlyAllowance true then take number of hours from lateAllowanceStartTime * hourlyAllowance

  @Column(precision = 12, scale = 2)
  private BigDecimal hourlyAllowance; // if isHourlyAllowance true then hourlyAllowance will be applied for lateAllowanceStartTime to allowanceCutOffHrs, if false then lateAllowanceCharges will be applied for lateAllowanceStartTime to allowanceCutOffHrs 
  
  @JsonFormat(pattern = "HH:mm")
  private LocalTime earlyAllowanceStartTime;
  
  @JsonFormat(pattern = "HH:mm")
  private LocalTime lateAllowanceStartTime;
  
  private Integer noOfDaysHourCutoff; // only for outstation and monthly rental

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

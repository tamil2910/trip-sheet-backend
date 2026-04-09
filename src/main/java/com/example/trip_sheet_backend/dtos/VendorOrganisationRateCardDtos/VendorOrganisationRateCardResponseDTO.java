package com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationRateCardResponseDTO {
  private UUID id;
  private UUID vendorId;
  private UUID vendorOrganisationId;
  private UUID vehicleTypeId;
  private UUID dutyTypeId;
  private String city;
  private BigDecimal baseFare;
  private BigDecimal extraKmCharges;
  private BigDecimal extraHrCharges;
  private BigDecimal dailyAllowanceCharges;
  private BigDecimal earlyAllowanceCharges;
  private BigDecimal lateAllowanceCharges;
  private Integer switchCutOffHrs;
  private Integer switchCutOffKms;
  private UUID switchDutyTypeId;
  private BigDecimal hourlyAllowance;
  private UUID noShowDutyTypeId;
  private Integer noOfDaysHourCutoff;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime earlyAllowanceStartTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime lateAllowanceStartTime;

  private Integer allowanceCutOffHrs;
  private VendorOrganisationRateCard.ApprovalStatus approvalStatus;
  private Long approvedAt;
  private String approvedBy;

  public static VendorOrganisationRateCardResponseDTO fromEntity(VendorOrganisationRateCard rateCard) {
    return new VendorOrganisationRateCardResponseDTO(
        rateCard.getId(),
        rateCard.getVendor().getId(),
        rateCard.getVendorOrganisation().getId(),
        rateCard.getVehicleType().getId(),
        rateCard.getDutyType().getId(),
        rateCard.getCity(),
        rateCard.getBaseFare(),
        rateCard.getExtraKmCharges(),
        rateCard.getExtraHrCharges(),
        rateCard.getDailyAllowanceCharges(),
        rateCard.getEarlyAllowanceCharges(),
        rateCard.getLateAllowanceCharges(),
        rateCard.getSwitchCutOffHrs(),
        rateCard.getSwitchCutOffKms(),
        rateCard.getSwitchDutyType() != null ? rateCard.getSwitchDutyType().getId() : null,
        rateCard.getHourlyAllowance(),
        rateCard.getNoShowDutyType() != null ? rateCard.getNoShowDutyType().getId() : null,
        rateCard.getNoOfDaysHourCutoff(),
        rateCard.getEarlyAllowanceStartTime(),
        rateCard.getLateAllowanceStartTime(),
        rateCard.getAllowanceCutOffHrs(),
        rateCard.getApprovalStatus(),
        rateCard.getApprovedAt(),
        rateCard.getApprovedBy()
    );
  }
}

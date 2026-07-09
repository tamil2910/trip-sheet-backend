package com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorOrganisation;
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
  private TenantSummaryDTO vendor;
  private VendorOrganisationSummaryDTO vendorOrganisation;
  private List<RelatedRateCardSummaryDTO> rateCards;

  public static VendorOrganisationRateCardResponseDTO fromEntity(VendorOrganisationRateCard rateCard) {
    return rateCard == null ? null : fromVendorOrganisation(rateCard.getVendorOrganisation());
  }

  public static VendorOrganisationRateCardResponseDTO fromVendorOrganisation(VendorOrganisation vendorOrganisation) {
    if (vendorOrganisation == null) {
      return null;
    }

    List<VendorOrganisationRateCard> activeRateCards = vendorOrganisation.getRateCards() == null
        ? List.of()
        : vendorOrganisation.getRateCards().stream()
            .filter(rateCard -> !Boolean.TRUE.equals(rateCard.getIsDeleted()))
            .toList();


    return new VendorOrganisationRateCardResponseDTO(
        vendorOrganisation.getId(),
        TenantSummaryDTO.fromEntity(vendorOrganisation.getVendor()),
        VendorOrganisationSummaryDTO.fromEntity(vendorOrganisation),
        activeRateCards.stream()
            .map(RelatedRateCardSummaryDTO::fromEntity)
            .toList()
    );
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TenantSummaryDTO {
    private UUID id;
    private String tenantName;
    private String contactEmail;
    private Tenant.TenantType tenantType;

    public static TenantSummaryDTO fromEntity(Tenant tenant) {
      if (tenant == null) {
        return null;
      }

      return new TenantSummaryDTO(
          tenant.getId(),
          tenant.getTenantName(),
          tenant.getContactEmail(),
          tenant.getTenantType()
      );
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VendorOrganisationSummaryDTO {
    private UUID id;
    private Boolean active;
    private VendorOrganisation.ContractStatus contractStatus;
    private Long onboardedAt;
    private Integer paymentTimelineInDays;
    private String localBillingStructure;
    private Integer minGtgKmLimit;
    private Integer minGtgHrLimit;
    private Integer maxGtgKmLimit;
    private Integer maxGtgHrLimit;
    private Long contractStartDate;
    private Long contractEndDate;

    public static VendorOrganisationSummaryDTO fromEntity(VendorOrganisation vendorOrganisation) {
      if (vendorOrganisation == null) {
        return null;
      }

      return new VendorOrganisationSummaryDTO(
          vendorOrganisation.getId(),
          vendorOrganisation.getActive(),
          vendorOrganisation.getContractStatus(),
          vendorOrganisation.getOnboardedAt(),
          vendorOrganisation.getPaymentTimelineInDays(),
          vendorOrganisation.getLocalBillingStructure(),
          vendorOrganisation.getMinGtgKmLimit(),
          vendorOrganisation.getMinGtgHrLimit(),
          vendorOrganisation.getMaxGtgKmLimit(),
          vendorOrganisation.getMaxGtgHrLimit(),
          vendorOrganisation.getContractStartDate(),
          vendorOrganisation.getContractEndDate()
      );
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RelatedRateCardSummaryDTO {
    private UUID id;
    private VehicleTypeSummaryDTO vehicleType;
    private DutyTypeSummaryDTO dutyType;
    private String city;
    private BigDecimal baseFare;
    private BigDecimal extraKmCharges;
    private BigDecimal extraHrCharges;
    private BigDecimal dailyAllowanceCharges;
    private BigDecimal earlyAllowanceCharges;
    private BigDecimal lateAllowanceCharges;
    private Integer switchCutOffHrs;
    private Integer switchCutOffKms;
    private DutyTypeSummaryDTO switchDutyType;
    private BigDecimal hourlyAllowance;
    private DutyTypeSummaryDTO noShowDutyType;
    private Integer noOfDaysHourCutoff;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime earlyAllowanceStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime lateAllowanceStartTime;

    private Integer allowanceCutOffHrs;
    private VendorOrganisationRateCard.ApprovalStatus approvalStatus;
    private Long approvedAt;
    private String approvedBy;

    public static RelatedRateCardSummaryDTO fromEntity(VendorOrganisationRateCard rateCard) {
      if (rateCard == null) {
        return null;
      }

      return new RelatedRateCardSummaryDTO(
          rateCard.getId(),
          VehicleTypeSummaryDTO.fromEntity(rateCard.getVehicleType()),
          DutyTypeSummaryDTO.fromEntity(rateCard.getDutyType()),
          rateCard.getCity(),
          rateCard.getBaseFare(),
          rateCard.getExtraKmCharges(),
          rateCard.getExtraHrCharges(),
          rateCard.getDailyAllowanceCharges(),
          rateCard.getEarlyAllowanceCharges(),
          rateCard.getLateAllowanceCharges(),
          rateCard.getSwitchCutOffHrs(),
          rateCard.getSwitchCutOffKms(),
          DutyTypeSummaryDTO.fromEntity(rateCard.getSwitchDutyType()),
          rateCard.getHourlyAllowance(),
          DutyTypeSummaryDTO.fromEntity(rateCard.getNoShowDutyType()),
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

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VehicleTypeSummaryDTO {
    private UUID id;
    private String defaultName;
    private Integer seatCount;
    private VehicleType.typeVehicle typeOfVehicle;

    public static VehicleTypeSummaryDTO fromEntity(VehicleType vehicleType) {
      if (vehicleType == null) {
        return null;
      }

      return new VehicleTypeSummaryDTO(
          vehicleType.getId(),
          vehicleType.getDefaultName(),
          vehicleType.getSeatCount(),
          vehicleType.getTypeOfVehicle()
      );
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DutyTypeSummaryDTO {
    private UUID id;
    private String name;
    private DutyType.typeDuty typeOfDuty;
    private Integer km;
    private Integer hr;

    public static DutyTypeSummaryDTO fromEntity(DutyType dutyType) {
      if (dutyType == null) {
        return null;
      }

      return new DutyTypeSummaryDTO(
          dutyType.getId(),
          dutyType.getName(),
          dutyType.getTypeOfDuty(),
          dutyType.getKm(),
          dutyType.getHr()
      );
    }
  }
}

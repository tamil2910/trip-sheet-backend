package com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos;

import java.math.BigDecimal;
import java.util.Comparator;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPartnerRateCardResponseDTO {
  private UUID id;
  private TenantSummaryDTO primaryVendor;
  private TenantSummaryDTO raisedByVendor;
  private VendorPartnerSummaryDTO vendorPartner;
  private List<RelatedRateCardSummaryDTO> rateCards;

  public static VendorPartnerRateCardResponseDTO fromEntity(VendorPartnerRateCard rateCard) {
    return rateCard == null ? null : fromVendorPartner(rateCard.getVendorPartner());
  }

  public static VendorPartnerRateCardResponseDTO fromVendorPartner(VendorPartner vendorPartner) {
    if (vendorPartner == null) {
      return null;
    }

    List<VendorPartnerRateCard> activeRateCards = vendorPartner.getRateCards() == null
        ? List.of()
        : vendorPartner.getRateCards().stream()
            .filter(relatedRateCard -> !Boolean.TRUE.equals(relatedRateCard.getIsDeleted()))
            .toList();

    VendorPartnerRateCard anchorRateCard = activeRateCards.stream()
        .max(Comparator.comparingLong(VendorPartnerRateCardResponseDTO::getRateCardPriorityTimestamp))
        .orElse(null);

    return new VendorPartnerRateCardResponseDTO(
        vendorPartner.getId(),
        TenantSummaryDTO.fromEntity(vendorPartner.getPrimaryVendor()),
        TenantSummaryDTO.fromEntity(resolveRaisedByVendor(anchorRateCard)),
        VendorPartnerSummaryDTO.fromEntity(vendorPartner),
        activeRateCards.stream()
            .map(RelatedRateCardSummaryDTO::fromEntity)
            .toList()
    );
  }

  private static Tenant resolveRaisedByVendor(VendorPartnerRateCard rateCard) {
    if (rateCard == null) {
      return null;
    }

    return rateCard.getRaisedByVendor() != null
        ? rateCard.getRaisedByVendor()
        : rateCard.getPrimaryVendor();
  }

  private static long getRateCardPriorityTimestamp(VendorPartnerRateCard rateCard) {
    if (rateCard == null) {
      return 0L;
    }

    if (rateCard.getUpdatedAt() != null) {
      return rateCard.getUpdatedAt();
    }

    if (rateCard.getCreatedAt() != null) {
      return rateCard.getCreatedAt();
    }

    return 0L;
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
  public static class VendorPartnerSummaryDTO {
    private UUID id;
    private VendorPartner.ContractStatus contractStatus;
    private Long onboardedAt;
    private Integer paymentTimelineInDays;
    private String localBillingStructure;
    private Integer minGtgKmLimit;
    private Integer minGtgHrLimit;
    private Integer maxGtgKmLimit;
    private Integer maxGtgHrLimit;
    private Long contractStartDate;
    private Long contractEndDate;

    public static VendorPartnerSummaryDTO fromEntity(VendorPartner vendorPartner) {
      if (vendorPartner == null) {
        return null;
      }

      return new VendorPartnerSummaryDTO(
          vendorPartner.getId(),
          vendorPartner.getContractStatus(),
          vendorPartner.getOnboardedAt(),
          vendorPartner.getPaymentTimelineInDays(),
          vendorPartner.getLocalBillingStructure(),
          vendorPartner.getMinGtgKmLimit(),
          vendorPartner.getMinGtgHrLimit(),
          vendorPartner.getMaxGtgKmLimit(),
          vendorPartner.getMaxGtgHrLimit(),
          vendorPartner.getContractStartDate(),
          vendorPartner.getContractEndDate()
      );
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RelatedRateCardSummaryDTO {
    private UUID id;
    private TenantSummaryDTO raisedByVendor;
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
    private LocalTime earlyAllowanceEndTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime lateAllowanceStartTime;

    private Integer allowanceCutOffHrs;
    private VendorPartnerRateCard.ApprovalStatus approvalStatus;
    private Long approvedAt;
    private String approvedBy;

    public static RelatedRateCardSummaryDTO fromEntity(VendorPartnerRateCard rateCard) {
      if (rateCard == null) {
        return null;
      }

      return new RelatedRateCardSummaryDTO(
          rateCard.getId(),
          TenantSummaryDTO.fromEntity(resolveRaisedByVendor(rateCard)),
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
          rateCard.getEarlyAllowanceEndTime(),
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

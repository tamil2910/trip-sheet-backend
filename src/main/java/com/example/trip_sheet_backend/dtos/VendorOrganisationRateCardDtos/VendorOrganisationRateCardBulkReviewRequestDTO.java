package com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationRateCardBulkReviewRequestDTO {

  @NotEmpty
  @Valid
  private List<RateCardActionDTO> rateCards;

  public enum BulkAction {
    APPROVE,
    REJECT,
    UPDATE
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RateCardActionDTO {
    @NotNull
    private UUID rateCardId;

    @NotNull
    private BulkAction action;

    @Valid
    private PartialUpdateDTO changes;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PartialUpdateDTO {
    private UUID vehicleTypeId;
    private UUID dutyTypeId;
    private String city;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal baseFare;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal extraKmCharges;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal extraHrCharges;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal dailyAllowanceCharges;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal earlyAllowanceCharges;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal lateAllowanceCharges;

    private Boolean isHourlyAllowance;

    private Integer switchCutOffHrs;
    private Integer switchCutOffKms;
    private UUID switchDutyTypeId;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal hourlyAllowance;

    private UUID noShowDutyTypeId;
    private Integer noOfDaysHourCutoff;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime earlyAllowanceEndTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime lateAllowanceStartTime;

    private Integer allowanceCutOffHrs;
  }
}

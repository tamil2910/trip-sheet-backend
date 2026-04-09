package com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrganisationRateCardCreateRequestDTO {

  @NotNull
  private UUID vehicleTypeId;

  @NotNull
  private UUID dutyTypeId;

  @NotBlank
  private String city;

  @NotNull
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

  private Integer switchCutOffHrs;

  private Integer switchCutOffKms;

  private UUID switchDutyTypeId;

  @DecimalMin(value = "0.0", inclusive = true)
  private BigDecimal hourlyAllowance;

  private UUID noShowDutyTypeId;

  private Integer noOfDaysHourCutoff;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime earlyAllowanceStartTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime lateAllowanceStartTime;

  private Integer allowanceCutOffHrs;
}

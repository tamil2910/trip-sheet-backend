package com.example.trip_sheet_backend.dtos.PurchaseOrderNumberRuleDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderNumberRuleRequestDto {
  @NotBlank(message = "period is required")
  @Pattern(regexp = "^\\d{4}(_\\d{4})?$", message = "period must be YYYY or YYYY_YYYY")
  private String period;

  @Pattern(regexp = "^$|^[A-Za-z0-9]+$", message = "suffix may contain only letters and numbers")
  private String suffix;

  @NotNull(message = "sequenceStart is required")
  @Positive(message = "sequenceStart must be greater than zero")
  private Long sequenceStart;

  private Boolean isDefault;
}

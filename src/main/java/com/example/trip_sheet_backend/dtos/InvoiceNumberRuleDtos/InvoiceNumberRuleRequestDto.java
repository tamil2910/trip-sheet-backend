package com.example.trip_sheet_backend.dtos.InvoiceNumberRuleDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceNumberRuleRequestDto {
  @NotBlank(message = "prefix is required")
  private String prefix;

  private String suffix;

  @NotNull(message = "sequenceStart is required")
  @Positive(message = "sequenceStart must be greater than zero")
  private Long sequenceStart;

  /** Set true to make this rule the vendor tenant's default rule. */
  private Boolean isDefault;
}

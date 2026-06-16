package com.example.trip_sheet_backend.dtos.TripBillingRuleDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.TripBillingRule;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripBillingRuleUpdateRequestDTO {

  @NotNull(message = "billingBasis is required")
  private TripBillingRule.BillingBasis billingBasis;

  private UUID costCenterCustomFieldId;

  @NotNull(message = "invoiceGrouping is required")
  private TripBillingRule.InvoiceGrouping invoiceGrouping;

  private Boolean active = true;
}

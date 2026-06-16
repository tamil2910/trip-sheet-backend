package com.example.trip_sheet_backend.dtos.TripBillingRuleDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.TripBillingRule;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripBillingRuleResponseDTO {
  private UUID id;
  private UUID tenantId;
  private TripBillingRule.BillingBasis billingBasis;
  private UUID costCenterCustomFieldId;
  private String costCenterCustomFieldName;
  private TripBillingRule.InvoiceGrouping invoiceGrouping;
  private Boolean active;

  public static TripBillingRuleResponseDTO fromEntity(TripBillingRule entity) {
    CustomField customField = entity.getCostCenterCustomField();
    return new TripBillingRuleResponseDTO(
        entity.getId(),
        entity.getTenant() == null ? null : entity.getTenant().getId(),
        entity.getBillingBasis(),
        customField == null ? null : customField.getId(),
        customField == null ? null : customField.getName(),
        entity.getInvoiceGrouping(),
        entity.getActive()
    );
  }
}

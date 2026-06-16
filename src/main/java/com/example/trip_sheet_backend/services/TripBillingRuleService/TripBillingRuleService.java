package com.example.trip_sheet_backend.services.TripBillingRuleService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TripBillingRule;

public interface TripBillingRuleService {
  TripBillingRule createRule(TripBillingRuleCreateRequestDTO body, Tenant tokenTenant, UUID createdBy);

  List<TripBillingRule> getRulesByTenant(Tenant tokenTenant);

  TripBillingRule getRuleById(UUID ruleId, Tenant tokenTenant);

  TripBillingRule getActiveRule(Tenant tokenTenant);

  TripBillingRule updateRule(UUID ruleId, TripBillingRuleUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy);

  void deleteRule(UUID ruleId, Tenant tokenTenant, UUID deletedBy);
}

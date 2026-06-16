package com.example.trip_sheet_backend.services.TripBillingRuleService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.TripBillingRuleDtos.TripBillingRuleUpdateRequestDTO;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TripBillingRule;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.TripBillingRuleRepository;

@Service
public class TripBillingRuleServiceImp implements TripBillingRuleService {

  private final TripBillingRuleRepository tripBillingRuleRepository;
  private final CustomFieldRepository customFieldRepository;

  public TripBillingRuleServiceImp(
      TripBillingRuleRepository tripBillingRuleRepository,
      CustomFieldRepository customFieldRepository
  ) {
    this.tripBillingRuleRepository = tripBillingRuleRepository;
    this.customFieldRepository = customFieldRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripBillingRule createRule(TripBillingRuleCreateRequestDTO body, Tenant tokenTenant, UUID createdBy) {
    validateTenant(tokenTenant);

    TripBillingRule rule = new TripBillingRule();
    rule.setTenant(tokenTenant);
    applyRuleFields(rule, body.getBillingBasis(), body.getCostCenterCustomFieldId(), body.getInvoiceGrouping(), body.getActive(), tokenTenant);
    if (createdBy != null) {
      rule.setCreatedBy(createdBy.toString());
      rule.setUpdatedBy(createdBy.toString());
    }

    TripBillingRule savedRule = tripBillingRuleRepository.save(rule);
    if (Boolean.TRUE.equals(savedRule.getActive())) {
      deactivateOtherRules(savedRule.getId(), tokenTenant.getId(), createdBy);
    }
    return savedRule;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TripBillingRule> getRulesByTenant(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return tripBillingRuleRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public TripBillingRule getRuleById(UUID ruleId, Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return tripBillingRuleRepository.findByIdAndTenant_IdAndIsDeletedFalse(ruleId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Trip billing rule not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public TripBillingRule getActiveRule(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return tripBillingRuleRepository.findFirstByTenant_IdAndActiveTrueAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId())
        .orElse(null);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TripBillingRule updateRule(UUID ruleId, TripBillingRuleUpdateRequestDTO body, Tenant tokenTenant, UUID updatedBy) {
    validateTenant(tokenTenant);

    TripBillingRule existingRule = tripBillingRuleRepository.findByIdAndTenant_IdAndIsDeletedFalse(ruleId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Trip billing rule not found"));

    applyRuleFields(existingRule, body.getBillingBasis(), body.getCostCenterCustomFieldId(), body.getInvoiceGrouping(), body.getActive(), tokenTenant);
    if (updatedBy != null) {
      existingRule.setUpdatedBy(updatedBy.toString());
    }

    TripBillingRule savedRule = tripBillingRuleRepository.save(existingRule);
    if (Boolean.TRUE.equals(savedRule.getActive())) {
      deactivateOtherRules(savedRule.getId(), tokenTenant.getId(), updatedBy);
    }
    return savedRule;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteRule(UUID ruleId, Tenant tokenTenant, UUID deletedBy) {
    validateTenant(tokenTenant);

    TripBillingRule existingRule = tripBillingRuleRepository.findByIdAndTenant_IdAndIsDeletedFalse(ruleId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Trip billing rule not found"));

    existingRule.setIsDeleted(true);
    existingRule.setDeletedAt(System.currentTimeMillis());
    existingRule.setActive(false);
    if (deletedBy != null) {
      existingRule.setDeletedBy(deletedBy.toString());
      existingRule.setUpdatedBy(deletedBy.toString());
    }
    tripBillingRuleRepository.save(existingRule);
  }

  private void applyRuleFields(
      TripBillingRule rule,
      TripBillingRule.BillingBasis billingBasis,
      UUID costCenterCustomFieldId,
      TripBillingRule.InvoiceGrouping invoiceGrouping,
      Boolean active,
      Tenant tokenTenant
  ) {
    if (billingBasis == TripBillingRule.BillingBasis.CUSTOM_FIELD_WISE && costCenterCustomFieldId == null) {
      throw new RuntimeException("costCenterCustomFieldId is required for CUSTOM_FIELD_WISE billing");
    }

    CustomField customField = null;
    if (costCenterCustomFieldId != null) {
      customField = customFieldRepository.findByIdAndTenant_Id(costCenterCustomFieldId, tokenTenant.getId())
          .orElseThrow(() -> new RuntimeException("Custom field not found for tenant"));
    }

    if (billingBasis == TripBillingRule.BillingBasis.TRIP_WISE) {
      customField = null;
    }

    rule.setBillingBasis(billingBasis);
    rule.setCostCenterCustomField(customField);
    rule.setInvoiceGrouping(invoiceGrouping);
    rule.setActive(active == null ? Boolean.TRUE : active);
  }

  private void deactivateOtherRules(UUID currentRuleId, UUID tenantId, UUID updatedBy) {
    List<TripBillingRule> existingRules = tripBillingRuleRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tenantId);
    for (TripBillingRule rule : existingRules) {
      if (rule.getId().equals(currentRuleId)) {
        continue;
      }
      if (Boolean.TRUE.equals(rule.getActive())) {
        rule.setActive(false);
        if (updatedBy != null) {
          rule.setUpdatedBy(updatedBy.toString());
        }
        tripBillingRuleRepository.save(rule);
      }
    }
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }
}

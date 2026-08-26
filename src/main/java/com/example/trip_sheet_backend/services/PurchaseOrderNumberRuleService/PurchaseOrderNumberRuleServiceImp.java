package com.example.trip_sheet_backend.services.PurchaseOrderNumberRuleService;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.PurchaseOrderNumberRuleDtos.PurchaseOrderNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.PurchaseOrderNumberRule;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.PurchaseOrderNumberRuleRepository;

@Service
public class PurchaseOrderNumberRuleServiceImp implements PurchaseOrderNumberRuleService {
  private final PurchaseOrderNumberRuleRepository ruleRepository;

  public PurchaseOrderNumberRuleServiceImp(PurchaseOrderNumberRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  @Override
  @Transactional
  public PurchaseOrderNumberRule create(PurchaseOrderNumberRuleRequestDto body, Tenant vendor, UUID createdBy) {
    validateVendor(vendor);
    PurchaseOrderNumberRule rule = new PurchaseOrderNumberRule();
    applyFields(rule, body, true);
    rule.setVendor(vendor);
    rule.setIsDefault(ruleRepository.countByVendor_IdAndIsDeletedFalse(vendor.getId()) == 0
        || Boolean.TRUE.equals(body.getIsDefault()));
    if (Boolean.TRUE.equals(rule.getIsDefault())) clearDefault(vendor.getId(), null, createdBy);
    setCreatedAudit(rule, createdBy);
    return ruleRepository.save(rule);
  }

  @Override
  public List<PurchaseOrderNumberRule> getAll(Tenant vendor) {
    validateVendor(vendor);
    return ruleRepository.findByVendor_IdAndIsDeletedFalseOrderByUpdatedAtDesc(vendor.getId());
  }

  @Override
  public PurchaseOrderNumberRule getById(UUID id, Tenant vendor) {
    validateVendor(vendor);
    return ruleRepository.findByIdAndVendor_IdAndIsDeletedFalse(id, vendor.getId())
        .orElseThrow(() -> new RuntimeException("Purchase order number rule not found"));
  }

  @Override
  @Transactional
  public PurchaseOrderNumberRule update(UUID id, PurchaseOrderNumberRuleRequestDto body, Tenant vendor, UUID updatedBy) {
    PurchaseOrderNumberRule rule = getById(id, vendor);
    if (Boolean.TRUE.equals(rule.getIsDefault()) && Boolean.FALSE.equals(body.getIsDefault())) {
      throw new RuntimeException("A default purchase order number rule must always be configured");
    }
    applyFields(rule, body, false);
    if (Boolean.TRUE.equals(body.getIsDefault()) && !Boolean.TRUE.equals(rule.getIsDefault())) {
      clearDefault(vendor.getId(), rule.getId(), updatedBy);
      rule.setIsDefault(true);
    }
    setUpdatedAudit(rule, updatedBy);
    return ruleRepository.save(rule);
  }

  @Override
  @Transactional
  public void delete(UUID id, Tenant vendor, UUID deletedBy) {
    PurchaseOrderNumberRule rule = getById(id, vendor);
    if (Boolean.TRUE.equals(rule.getIsDefault())) {
      throw new RuntimeException("Select another default purchase order number rule before deleting this rule");
    }
    rule.setIsDeleted(true);
    if (deletedBy != null) rule.setDeletedBy(deletedBy.toString());
    ruleRepository.save(rule);
  }

  private void applyFields(PurchaseOrderNumberRule rule, PurchaseOrderNumberRuleRequestDto body, boolean creating) {
    rule.setPeriod(body.getPeriod().trim());
    rule.setSuffix(body.getSuffix() == null || body.getSuffix().isBlank() ? null : body.getSuffix().trim());
    if (creating) {
      rule.setSequenceStart(body.getSequenceStart());
      rule.setNextSequence(body.getSequenceStart());
      rule.setNextCombinedSequence(body.getSequenceStart());
    }
  }

  private void clearDefault(UUID vendorId, UUID excludedId, UUID actorId) {
    ruleRepository.findByVendor_IdAndIsDefaultTrueAndIsDeletedFalse(vendorId)
        .filter(rule -> excludedId == null || !rule.getId().equals(excludedId))
        .ifPresent(rule -> {
          rule.setIsDefault(false);
          setUpdatedAudit(rule, actorId);
          ruleRepository.save(rule);
        });
  }

  private void validateVendor(Tenant vendor) {
    if (vendor == null || vendor.getId() == null) throw new RuntimeException("Vendor not found in token");
    if (vendor.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenants can manage purchase order number rules");
    }
  }

  private void setCreatedAudit(PurchaseOrderNumberRule rule, UUID actorId) {
    if (actorId != null) {
      rule.setCreatedBy(actorId.toString());
      rule.setUpdatedBy(actorId.toString());
    }
  }

  private void setUpdatedAudit(PurchaseOrderNumberRule rule, UUID actorId) {
    if (actorId != null) rule.setUpdatedBy(actorId.toString());
  }
}

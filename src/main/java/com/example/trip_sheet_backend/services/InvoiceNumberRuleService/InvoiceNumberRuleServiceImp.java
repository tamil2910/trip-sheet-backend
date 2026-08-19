package com.example.trip_sheet_backend.services.InvoiceNumberRuleService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.InvoiceNumberRuleDtos.InvoiceNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.InvoiceNumberRule;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.InvoiceNumberRuleRepository;

@Service
public class InvoiceNumberRuleServiceImp implements InvoiceNumberRuleService {
  private final InvoiceNumberRuleRepository invoiceNumberRuleRepository;

  public InvoiceNumberRuleServiceImp(InvoiceNumberRuleRepository invoiceNumberRuleRepository) {
    this.invoiceNumberRuleRepository = invoiceNumberRuleRepository;
  }

  @Override
  @Transactional
  public InvoiceNumberRule create(InvoiceNumberRuleRequestDto body, Tenant tenant, UUID createdBy) {
    validateVendorTenant(tenant);
    InvoiceNumberRule rule = new InvoiceNumberRule();
    applyFields(rule, body);
    rule.setFinancialYear(currentFinancialYear());
    rule.setNextSequence(rule.getSequenceStart());
    rule.setTenant(tenant);
    rule.setIsDefault(invoiceNumberRuleRepository.countByTenant_IdAndIsDeletedFalse(tenant.getId()) == 0
        || Boolean.TRUE.equals(body.getIsDefault()));
    if (Boolean.TRUE.equals(rule.getIsDefault())) {
      clearDefaultRule(tenant.getId(), null, createdBy);
    }
    setCreatedAudit(rule, createdBy);
    return invoiceNumberRuleRepository.save(rule);
  }

  @Override
  public List<InvoiceNumberRule> getAll(Tenant tenant) {
    validateVendorTenant(tenant);
    return invoiceNumberRuleRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tenant.getId());
  }

  @Override
  public InvoiceNumberRule getById(UUID id, Tenant tenant) {
    validateVendorTenant(tenant);
    return invoiceNumberRuleRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .orElseThrow(() -> new RuntimeException("Invoice number rule not found"));
  }

  @Override
  @Transactional
  public InvoiceNumberRule update(UUID id, InvoiceNumberRuleRequestDto body, Tenant tenant, UUID updatedBy) {
    InvoiceNumberRule rule = getById(id, tenant);
    boolean wasDefault = Boolean.TRUE.equals(rule.getIsDefault());
    if (wasDefault && Boolean.FALSE.equals(body.getIsDefault())) {
      throw new RuntimeException("A default invoice number rule must always be configured");
    }
    applyFields(rule, body);
    if (Boolean.TRUE.equals(body.getIsDefault()) && !wasDefault) {
      clearDefaultRule(tenant.getId(), rule.getId(), updatedBy);
      rule.setIsDefault(true);
    }
    setUpdatedAudit(rule, updatedBy);
    return invoiceNumberRuleRepository.save(rule);
  }

  @Override
  @Transactional
  public void delete(UUID id, Tenant tenant, UUID deletedBy) {
    InvoiceNumberRule rule = getById(id, tenant);
    if (Boolean.TRUE.equals(rule.getIsDefault())) {
      throw new RuntimeException("Select another default invoice number rule before deleting this rule");
    }
    rule.setIsDeleted(true);
    if (deletedBy != null) {
      rule.setDeletedBy(deletedBy.toString());
    }
    invoiceNumberRuleRepository.save(rule);
  }

  private void applyFields(InvoiceNumberRule rule, InvoiceNumberRuleRequestDto body) {
    rule.setPrefix(body.getPrefix().trim());
    rule.setSuffix(body.getSuffix() == null || body.getSuffix().isBlank() ? null : body.getSuffix().trim());
    rule.setSequenceStart(body.getSequenceStart());
    if (rule.getNextSequence() == null) {
      rule.setNextSequence(body.getSequenceStart());
    }
  }

  private void clearDefaultRule(UUID tenantId, UUID excludedId, UUID actorId) {
    invoiceNumberRuleRepository.findByTenant_IdAndIsDefaultTrueAndIsDeletedFalse(tenantId)
        .filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
        .ifPresent(existing -> {
          existing.setIsDefault(false);
          setUpdatedAudit(existing, actorId);
          invoiceNumberRuleRepository.save(existing);
        });
  }

  private void validateVendorTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
    if (tenant.getTenantType() != Tenant.TenantType.VENDOR) {
      throw new RuntimeException("Only vendor tenants can manage invoice number rules");
    }
  }

  private String currentFinancialYear() {
    LocalDate today = LocalDate.now();
    int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
    return startYear + "_" + (startYear + 1);
  }

  private void setCreatedAudit(InvoiceNumberRule rule, UUID actorId) {
    if (actorId != null) {
      rule.setCreatedBy(actorId.toString());
      rule.setUpdatedBy(actorId.toString());
    }
  }

  private void setUpdatedAudit(InvoiceNumberRule rule, UUID actorId) {
    if (actorId != null) {
      rule.setUpdatedBy(actorId.toString());
    }
  }
}

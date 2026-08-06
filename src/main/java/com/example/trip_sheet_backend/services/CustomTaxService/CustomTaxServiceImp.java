package com.example.trip_sheet_backend.services.CustomTaxService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.CustomTaxDtos.CustomTaxRequestDto;
import com.example.trip_sheet_backend.models.CustomTax;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.CustomTaxRepository;
import com.example.trip_sheet_backend.repositories.TaxRepository;

@Service
public class CustomTaxServiceImp implements CustomTaxService {
  private final CustomTaxRepository customTaxRepository;
  private final TaxRepository taxRepository;

  public CustomTaxServiceImp(CustomTaxRepository customTaxRepository, TaxRepository taxRepository) {
    this.customTaxRepository = customTaxRepository;
    this.taxRepository = taxRepository;
  }

  @Override
  @Transactional
  public CustomTax create(CustomTaxRequestDto body, Tenant tenant, UUID createdBy) {
    validateTenant(tenant);
    Tax tax = resolveOrCreateTax(body, createdBy);
    customTaxRepository.findByTenant_IdAndTax_IdAndIsDeletedFalse(tenant.getId(), tax.getId())
        .ifPresent(existing -> { throw new RuntimeException("Custom tax already exists for this tenant"); });

    CustomTax customTax = new CustomTax();
    customTax.setTenant(tenant);
    customTax.setTax(tax);
    customTax.setCustomTaxName(normalizeCustomTaxName(body.getCustomTaxName()));
    customTax.setIsActive(true);
    setAuditUsers(customTax, createdBy);
    return customTaxRepository.save(customTax);
  }

  @Override
  public List<CustomTax> getAll(Tenant tenant) {
    validateTenant(tenant);
    return customTaxRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tenant.getId());
  }

  @Override
  public CustomTax getById(UUID id, Tenant tenant) {
    validateTenant(tenant);
    return customTaxRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .orElseThrow(() -> new RuntimeException("Custom tax not found"));
  }

  @Override
  @Transactional
  public CustomTax update(UUID id, CustomTaxRequestDto body, Tenant tenant, UUID updatedBy) {
    CustomTax customTax = getById(id, tenant);
    Tax tax = resolveOrCreateTax(body, updatedBy);
    customTaxRepository.findByTenant_IdAndTax_IdAndIsDeletedFalse(tenant.getId(), tax.getId())
        .ifPresent(existing -> {
          if (!existing.getId().equals(customTax.getId())) {
            throw new RuntimeException("Custom tax already exists for this tenant");
          }
        });

    customTax.setTax(tax);
    customTax.setCustomTaxName(normalizeCustomTaxName(body.getCustomTaxName()));
    if (updatedBy != null) {
      customTax.setUpdatedBy(updatedBy.toString());
    }
    return customTaxRepository.save(customTax);
  }

  @Override
  public void delete(UUID id, Tenant tenant, UUID deletedBy) {
    CustomTax customTax = getById(id, tenant);
    customTax.setIsDeleted(true);
    if (deletedBy != null) {
      customTax.setDeletedBy(deletedBy.toString());
    }
    customTaxRepository.save(customTax);
  }

  private Tax resolveOrCreateTax(CustomTaxRequestDto body, UUID actorId) {
    BigDecimal percentage = body.getTaxPercentage().stripTrailingZeros();
    String taxName = body.getTaxName().trim();
    return taxRepository.findByTaxNameIgnoreCaseAndTaxPercentageAndTaxType(taxName, percentage, body.getTaxType())
        .filter(tax -> !Boolean.TRUE.equals(tax.getIsDeleted()))
        .orElseGet(() -> {
          Tax tax = new Tax();
          tax.setTaxName(taxName);
          tax.setTaxPercentage(percentage);
          tax.setTaxType(body.getTaxType());
          tax.setIsActive(true);
          if (actorId != null) {
            tax.setCreatedBy(actorId.toString());
            tax.setUpdatedBy(actorId.toString());
          }
          return taxRepository.save(tax);
        });
  }

  private void validateTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private String normalizeCustomTaxName(String customTaxName) {
    if (customTaxName == null || customTaxName.isBlank()) {
      throw new RuntimeException("customTaxName is required");
    }
    return customTaxName.trim();
  }

  private void setAuditUsers(CustomTax customTax, UUID actorId) {
    if (actorId != null) {
      customTax.setCreatedBy(actorId.toString());
      customTax.setUpdatedBy(actorId.toString());
    }
  }
}

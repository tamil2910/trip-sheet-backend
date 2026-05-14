package com.example.trip_sheet_backend.services.TaxService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.dtos.TaxDtos.CreateTaxRequestDto;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.TaxRepository;

@Service
public class TaxServiceImp extends BaseServiceImp<Tax, UUID> implements TaxService {

  private final TaxRepository taxRepository;

  public TaxServiceImp(TaxRepository taxRepository) {
    super(taxRepository);
    this.taxRepository = taxRepository;
  }

  @Override
  public Tax createTax(CreateTaxRequestDto body, Tenant tokenTenant, UUID createdBy) {
    validateTenant(tokenTenant);

    BigDecimal normalizedPercentage = body.getTaxPercentage().stripTrailingZeros();
    String generatedTaxName = buildTaxName(normalizedPercentage, body.getTaxType());

    taxRepository.findByTenant_IdAndTaxNameIgnoreCase(tokenTenant.getId(), generatedTaxName)
        .ifPresent(existing -> {
          throw new RuntimeException("Tax already exists for this tenant: " + generatedTaxName);
        });

    Tax tax = new Tax();
    tax.setTaxPercentage(normalizedPercentage);
    tax.setTaxType(body.getTaxType());
    tax.setTaxName(generatedTaxName);
    tax.setTenant(tokenTenant);
    tax.setIsActive(true);
    if (createdBy != null) {
      tax.setCreatedBy(createdBy.toString());
      tax.setUpdatedBy(createdBy.toString());
    }

    return taxRepository.save(tax);
  }

  @Override
  public List<Tax> getTaxesByTenant(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return taxRepository.findByTenant_IdOrderByTaxPercentageAscTaxTypeAsc(tokenTenant.getId());
  }

  @Override
  public Tax updateTax(UUID taxId, CreateTaxRequestDto body, Tenant tokenTenant, UUID updatedBy) {
    validateTenant(tokenTenant);

    Tax existingTax = taxRepository.findByIdAndTenant_Id(taxId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Tax not found"));

    BigDecimal normalizedPercentage = body.getTaxPercentage().stripTrailingZeros();
    String generatedTaxName = buildTaxName(normalizedPercentage, body.getTaxType());

    taxRepository.findByTenant_IdAndTaxNameIgnoreCase(tokenTenant.getId(), generatedTaxName)
        .ifPresent(duplicateTax -> {
          if (!duplicateTax.getId().equals(existingTax.getId())) {
            throw new RuntimeException("Tax already exists for this tenant: " + generatedTaxName);
          }
        });

    existingTax.setTaxPercentage(normalizedPercentage);
    existingTax.setTaxType(body.getTaxType());
    existingTax.setTaxName(generatedTaxName);
    if (updatedBy != null) {
      existingTax.setUpdatedBy(updatedBy.toString());
    }

    return taxRepository.save(existingTax);
  }

  @Override
  public void deleteTax(UUID taxId, Tenant tokenTenant) {
    validateTenant(tokenTenant);

    Tax existingTax = taxRepository.findByIdAndTenant_Id(taxId, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Tax not found"));

    taxRepository.delete(existingTax);
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private String buildTaxName(BigDecimal taxPercentage, Tax.TaxType taxType) {
    return taxPercentage.stripTrailingZeros().toPlainString() + "% " + taxType.name();
  }
}

package com.example.trip_sheet_backend.services.TaxService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.dtos.TaxDtos.CreateTaxRequestDto;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.repositories.TaxRepository;

@Service
public class TaxServiceImp extends GlobalBaseServiceImp<Tax, UUID> implements TaxService {

  private final TaxRepository taxRepository;

  public TaxServiceImp(TaxRepository taxRepository) {
    super(taxRepository);
    this.taxRepository = taxRepository;
  }

  @Override
  public Tax createTax(CreateTaxRequestDto body, UUID createdBy) {
    BigDecimal normalizedPercentage = body.getTaxPercentage().stripTrailingZeros();
    String normalizedTaxName = normalizeTaxName(body.getTaxName());

    taxRepository.findByTaxNameIgnoreCaseAndTaxPercentageAndTaxType(
        normalizedTaxName, normalizedPercentage, body.getTaxType())
        .ifPresent(existing -> {
          throw new RuntimeException("Tax already exists: " + normalizedTaxName);
        });

    Tax tax = new Tax();
    tax.setTaxPercentage(normalizedPercentage);
    tax.setTaxType(body.getTaxType());
    tax.setTaxName(normalizedTaxName);
    tax.setIsActive(true);
    if (createdBy != null) {
      tax.setCreatedBy(createdBy.toString());
      tax.setUpdatedBy(createdBy.toString());
    }

    return taxRepository.save(tax);
  }

  @Override
  public List<Tax> getTaxes() {
    return taxRepository.findByIsDeletedFalseOrderByTaxPercentageAscTaxTypeAsc();
  }

  @Override
  public Tax getTaxById(UUID taxId) {
    Tax tax = taxRepository.findById(taxId)
        .filter(existingTax -> !Boolean.TRUE.equals(existingTax.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Tax not found"));
    return tax;
  }

  @Override
  public Tax updateTax(UUID taxId, CreateTaxRequestDto body, UUID updatedBy) {
    Tax existingTax = getTaxById(taxId);

    BigDecimal normalizedPercentage = body.getTaxPercentage().stripTrailingZeros();
    String normalizedTaxName = normalizeTaxName(body.getTaxName());

    taxRepository.findByTaxNameIgnoreCaseAndTaxPercentageAndTaxType(
        normalizedTaxName, normalizedPercentage, body.getTaxType())
        .ifPresent(duplicateTax -> {
          if (!duplicateTax.getId().equals(existingTax.getId())) {
            throw new RuntimeException("Tax already exists: " + normalizedTaxName);
          }
        });

    existingTax.setTaxPercentage(normalizedPercentage);
    existingTax.setTaxType(body.getTaxType());
    existingTax.setTaxName(normalizedTaxName);
    if (updatedBy != null) {
      existingTax.setUpdatedBy(updatedBy.toString());
    }

    return taxRepository.save(existingTax);
  }

  @Override
  public void deleteTax(UUID taxId, UUID deletedBy) {
    Tax existingTax = getTaxById(taxId);
    existingTax.setIsDeleted(true);
    if (deletedBy != null) {
      existingTax.setDeletedBy(deletedBy.toString());
    }
    taxRepository.save(existingTax);
  }

  private String normalizeTaxName(String taxName) {
    if (taxName == null || taxName.isBlank()) {
      throw new RuntimeException("taxName is required");
    }
    return taxName.trim();
  }
}

package com.example.trip_sheet_backend.services.VendorOrganisationTaxService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationTaxDtos.VendorOrganisationTaxCreateRequestDto;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationTax;
import com.example.trip_sheet_backend.repositories.TaxRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationTaxRepository;

@Service
public class VendorOrganisationTaxServiceImp implements VendorOrganisationTaxService {

  private final VendorOrganisationTaxRepository vendorOrganisationTaxRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final TaxRepository taxRepository;

  public VendorOrganisationTaxServiceImp(
      VendorOrganisationTaxRepository vendorOrganisationTaxRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      TaxRepository taxRepository
  ) {
    this.vendorOrganisationTaxRepository = vendorOrganisationTaxRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.taxRepository = taxRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisationTax createVendorOrganisationTax(
      VendorOrganisationTaxCreateRequestDto body,
      Tenant tokenTenant,
      UUID createdBy
  ) {
    validateTenant(tokenTenant);

    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(body.getVendorOrganisationId(), tokenTenant);
    Tax tax = resolveOrCreateTax(body, tokenTenant, createdBy);

    if (vendorOrganisationTaxRepository.existsByTenant_IdAndVendorOrganisation_IdAndTax_Id(
        tokenTenant.getId(),
        vendorOrganisation.getId(),
        tax.getId())) {
      throw new RuntimeException("This vendor organisation tax already exists");
    }

    VendorOrganisationTax mapping = new VendorOrganisationTax();
    mapping.setTenant(tokenTenant);
    mapping.setVendorOrganisation(vendorOrganisation);
    mapping.setTax(tax);
    if (createdBy != null) {
      mapping.setCreatedBy(createdBy.toString());
      mapping.setUpdatedBy(createdBy.toString());
    }

    return vendorOrganisationTaxRepository.save(mapping);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorOrganisationTax> getVendorOrganisationTaxes(Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return vendorOrganisationTaxRepository.findByTenant_IdAndIsDeletedFalseOrderByUpdatedAtDesc(tokenTenant.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorOrganisationTax> getVendorOrganisationTaxesByVendorOrganisation(UUID vendorOrganisationId, Tenant tokenTenant) {
    validateTenant(tokenTenant);
    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(vendorOrganisationId, tokenTenant);
    return vendorOrganisationTaxRepository.findByVendorOrganisation_IdAndTenant_IdAndIsDeletedFalse(
        vendorOrganisation.getId(),
        tokenTenant.getId()
    );
  }

  @Override
  @Transactional(readOnly = true)
  public VendorOrganisationTax getVendorOrganisationTaxById(UUID id, Tenant tokenTenant) {
    validateTenant(tokenTenant);
    return vendorOrganisationTaxRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Vendor organisation tax not found"));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisationTax updateVendorOrganisationTax(
      UUID id,
      VendorOrganisationTaxCreateRequestDto body,
      Tenant tokenTenant,
      UUID updatedBy
  ) {
    validateTenant(tokenTenant);

    VendorOrganisationTax existing = vendorOrganisationTaxRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Vendor organisation tax not found"));

    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(body.getVendorOrganisationId(), tokenTenant);
    Tax tax = resolveOrCreateTax(body, tokenTenant, updatedBy);

    boolean duplicateExists = vendorOrganisationTaxRepository.existsByTenant_IdAndVendorOrganisation_IdAndTax_Id(
        tokenTenant.getId(),
        vendorOrganisation.getId(),
        tax.getId()
    );

    boolean changedCombination = !existing.getVendorOrganisation().getId().equals(vendorOrganisation.getId())
        || !existing.getTax().getId().equals(tax.getId());

    if (changedCombination && duplicateExists) {
      throw new RuntimeException("This vendor organisation tax already exists");
    }

    existing.setTenant(tokenTenant);
    existing.setVendorOrganisation(vendorOrganisation);
    existing.setTax(tax);
    if (updatedBy != null) {
      existing.setUpdatedBy(updatedBy.toString());
    }

    return vendorOrganisationTaxRepository.save(existing);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteVendorOrganisationTax(UUID id, Tenant tokenTenant, UUID deletedBy) {
    validateTenant(tokenTenant);

    VendorOrganisationTax existing = vendorOrganisationTaxRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tokenTenant.getId())
        .orElseThrow(() -> new RuntimeException("Vendor organisation tax not found"));

    existing.setIsDeleted(true);
    existing.setDeletedAt(Instant.now().toEpochMilli());
    if (deletedBy != null) {
      existing.setDeletedBy(deletedBy.toString());
      existing.setUpdatedBy(deletedBy.toString());
    }

    vendorOrganisationTaxRepository.save(existing);
  }

  private Tax resolveOrCreateTax(VendorOrganisationTaxCreateRequestDto body, Tenant tokenTenant, UUID actorId) {
    BigDecimal normalizedPercentage = body.getTaxPercentage().stripTrailingZeros();

    return taxRepository.findByTenant_IdAndTaxPercentageAndTaxType(
            tokenTenant.getId(),
            normalizedPercentage,
            body.getTaxType()
        )
        .orElseGet(() -> createTax(body, tokenTenant, actorId, normalizedPercentage));
  }

  private Tax createTax(
      VendorOrganisationTaxCreateRequestDto body,
      Tenant tokenTenant,
      UUID actorId,
      BigDecimal normalizedPercentage
  ) {
    Tax tax = new Tax();
    tax.setTaxPercentage(normalizedPercentage);
    tax.setTaxType(body.getTaxType());
    tax.setTaxName(buildTaxName(normalizedPercentage, body.getTaxType()));
    tax.setTenant(tokenTenant);
    tax.setIsActive(true);
    if (actorId != null) {
      tax.setCreatedBy(actorId.toString());
      tax.setUpdatedBy(actorId.toString());
    }
    return taxRepository.save(tax);
  }

  private VendorOrganisation resolveVendorOrganisation(UUID vendorOrganisationId, Tenant tokenTenant) {
    VendorOrganisation vendorOrganisation = vendorOrganisationRepository.findById(vendorOrganisationId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation not found"));

    boolean isVendor = vendorOrganisation.getVendor() != null
        && vendorOrganisation.getVendor().getId().equals(tokenTenant.getId());
    boolean isOrganisation = vendorOrganisation.getOrganisation() != null
        && vendorOrganisation.getOrganisation().getId().equals(tokenTenant.getId());

    if (!isVendor && !isOrganisation) {
      throw new RuntimeException("You are not allowed to access this vendor organisation");
    }

    return vendorOrganisation;
  }

  private void validateTenant(Tenant tokenTenant) {
    if (tokenTenant == null || tokenTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private String buildTaxName(BigDecimal taxPercentage, Tax.TaxType taxType) {
    return taxPercentage.stripTrailingZeros().toPlainString() + "% " + taxType.name();
  }
}

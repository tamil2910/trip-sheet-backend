package com.example.trip_sheet_backend.services.VendorOrganisationService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationDtos.VendorOrganisationUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.Tax;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.repositories.TaxRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class VendorOrganisationServiceImp implements VendorOrganisationService {
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final TaxRepository taxRepository;

  public VendorOrganisationServiceImp(VendorOrganisationRepository vendorOrganisationRepository,
      TaxRepository taxRepository) {
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.taxRepository = taxRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisation update(UUID vendorOrganisationId, VendorOrganisationUpdateRequestDTO body,
      Tenant loggedInTenant, UUID updatedBy) {
    VendorOrganisation vendorOrganisation = vendorOrganisationRepository.findById(vendorOrganisationId)
        .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));

    validateLinkedTenant(loggedInTenant, vendorOrganisation);
    applyUpdate(vendorOrganisation, body);
    if (updatedBy != null) {
      vendorOrganisation.setUpdatedBy(updatedBy.toString());
    }

    return vendorOrganisationRepository.save(vendorOrganisation);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisation updateTaxes(UUID vendorOrganisationId, List<UUID> taxIds,
      Tenant loggedInTenant, UUID updatedBy) {
    VendorOrganisation vendorOrganisation = vendorOrganisationRepository.findById(vendorOrganisationId)
        .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));

    validateLinkedTenant(loggedInTenant, vendorOrganisation);
    vendorOrganisation.setTaxList(resolveTaxes(taxIds));
    if (updatedBy != null) {
      vendorOrganisation.setUpdatedBy(updatedBy.toString());
    }
    return vendorOrganisationRepository.save(vendorOrganisation);
  }

  private void validateLinkedTenant(Tenant loggedInTenant, VendorOrganisation vendorOrganisation) {
    if (loggedInTenant == null || loggedInTenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID tenantId = loggedInTenant.getId();
    if (!tenantId.equals(vendorOrganisation.getVendor().getId())
        && !tenantId.equals(vendorOrganisation.getOrganisation().getId())) {
      throw new RuntimeException("You are not allowed to update this vendor organisation relationship");
    }
  }

  private void applyUpdate(VendorOrganisation target, VendorOrganisationUpdateRequestDTO body) {
    if (body.getActive() != null) target.setActive(body.getActive());
    if (body.getOnboardedAt() != null) target.setOnboardedAt(body.getOnboardedAt());
    if (body.getPaymentTimelineInDays() != null) target.setPaymentTimelineInDays(body.getPaymentTimelineInDays());
    if (body.getLocalBillingStructure() != null) target.setLocalBillingStructure(body.getLocalBillingStructure());
    if (body.getMinGtgKmLimit() != null) target.setMinGtgKmLimit(body.getMinGtgKmLimit());
    if (body.getMinGtgHrLimit() != null) target.setMinGtgHrLimit(body.getMinGtgHrLimit());
    if (body.getMaxGtgKmLimit() != null) target.setMaxGtgKmLimit(body.getMaxGtgKmLimit());
    if (body.getMaxGtgHrLimit() != null) target.setMaxGtgHrLimit(body.getMaxGtgHrLimit());
    if (body.getContractStatus() != null) target.setContractStatus(body.getContractStatus());
    if (body.getContractStartDate() != null) target.setContractStartDate(body.getContractStartDate());
    if (body.getContractEndDate() != null) target.setContractEndDate(body.getContractEndDate());
    if (body.getTaxIds() != null) target.setTaxList(resolveTaxes(body.getTaxIds()));
  }

  private List<Tax> resolveTaxes(List<UUID> taxIds) {
    if (taxIds == null) {
      return new ArrayList<>();
    }

    List<UUID> distinctTaxIds = new ArrayList<>(new LinkedHashSet<>(taxIds));
    if (distinctTaxIds.size() != taxIds.size()) {
      throw new RuntimeException("Duplicate tax ids are not allowed");
    }

    List<Tax> taxes = taxRepository.findAllById(distinctTaxIds);
    if (taxes.size() != distinctTaxIds.size()
        || taxes.stream().anyMatch(tax -> Boolean.TRUE.equals(tax.getIsDeleted()))) {
      throw new RuntimeException("One or more tax ids are invalid");
    }
    return taxes;
  }
}

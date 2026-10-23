package com.example.trip_sheet_backend.services.RateCardPackageService;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.trip_sheet_backend.dtos.RateCardPackageDtos.RateCardPackageCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.RateCardPackageDtos.RateCardPackageUpdateRequestDTO;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.models.ratecardpackage.RateCardPackage;
import com.example.trip_sheet_backend.models.ratecardpackage.RateCardPackageItem;
import com.example.trip_sheet_backend.repositories.RateCardPackageRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class RateCardPackageService {
  private final RateCardPackageRepository packages;
  private final VendorPartnerRepository partners;
  private final VendorOrganisationRepository organisations;
  private final VendorPartnerRateCardRepository partnerRates;
  private final VendorOrganisationRateCardRepository organisationRates;

  public RateCardPackageService(RateCardPackageRepository packages, VendorPartnerRepository partners,
      VendorOrganisationRepository organisations, VendorPartnerRateCardRepository partnerRates,
      VendorOrganisationRateCardRepository organisationRates) {
    this.packages = packages;
    this.partners = partners;
    this.organisations = organisations;
    this.partnerRates = partnerRates;
    this.organisationRates = organisationRates;
  }

  @Transactional(rollbackFor = Exception.class)
  public RateCardPackage create(RateCardPackageCreateRequestDTO body, Tenant tenant, UUID actorId) {
    if ((body.getVendorPartnerId() == null) == (body.getVendorOrganisationId() == null)) {
      throw new IllegalArgumentException("Provide exactly one of vendorPartnerId or vendorOrganisationId");
    }
    if (body.getRateCardIds().stream().distinct().count() != body.getRateCardIds().size()) {
      throw new IllegalArgumentException("Duplicate rateCardIds are not allowed");
    }

    RateCardPackage entity = new RateCardPackage();
    entity.setName(body.getName().trim());
    entity.setValidFrom(body.getValidFrom());
    entity.setValidTo(body.getValidTo());
    entity.setIsDefault(Boolean.TRUE.equals(body.getIsDefault()));
    entity.setCreatedBy(actorId == null ? null : actorId.toString());

    if (body.getVendorPartnerId() != null) {
      VendorPartner link = partners.findById(body.getVendorPartnerId())
          .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
          .orElseThrow(() -> new IllegalArgumentException("Vendor partner relationship not found"));
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(link.getPrimaryVendor().getId())) {
        throw new IllegalArgumentException("Only the primary vendor can manage partner rate card packages");
      }
      entity.setVendorPartner(link);
      List<VendorPartnerRateCard> rates = partnerRates.findAllById(body.getRateCardIds());
      if (rates.size() != body.getRateCardIds().size() || rates.stream().anyMatch(r ->
          Boolean.TRUE.equals(r.getIsDeleted()) || !link.getId().equals(r.getVendorPartner().getId()))) {
        throw new IllegalArgumentException("Every rate card must belong to the selected vendor partner");
      }
      rates.forEach(rate -> addPartnerRate(entity, rate));
      if (Boolean.TRUE.equals(entity.getIsDefault())) {
        validateDates(entity.getValidFrom(), entity.getValidTo(), true);
        link.setContractStartDate(entity.getValidFrom());
        link.setContractEndDate(entity.getValidTo());
        link.setUpdatedBy(actorId == null ? null : actorId.toString());
        packages.findByVendorPartner_IdAndIsDefaultTrueAndIsDeletedFalse(link.getId())
            .ifPresent(previous -> previous.setIsDefault(false));
      }
    } else {
      VendorOrganisation link = organisations.findById(body.getVendorOrganisationId())
          .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()))
          .orElseThrow(() -> new IllegalArgumentException("Vendor organisation relationship not found"));
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(link.getVendor().getId())) {
        throw new IllegalArgumentException("Only the vendor can manage organisation rate card packages");
      }
      entity.setVendorOrganisation(link);
      List<VendorOrganisationRateCard> rates = organisationRates.findAllById(body.getRateCardIds());
      if (rates.size() != body.getRateCardIds().size() || rates.stream().anyMatch(r ->
          Boolean.TRUE.equals(r.getIsDeleted()) || !link.getId().equals(r.getVendorOrganisation().getId()))) {
        throw new IllegalArgumentException("Every rate card must belong to the selected vendor organisation");
      }
      rates.forEach(rate -> addOrganisationRate(entity, rate));
      if (Boolean.TRUE.equals(entity.getIsDefault())) {
        validateDates(entity.getValidFrom(), entity.getValidTo(), true);
        link.setContractStartDate(entity.getValidFrom());
        link.setContractEndDate(entity.getValidTo());
        link.setUpdatedBy(actorId == null ? null : actorId.toString());
        packages.findByVendorOrganisation_IdAndIsDefaultTrueAndIsDeletedFalse(link.getId())
            .ifPresent(previous -> previous.setIsDefault(false));
      }
    }
    validateDates(entity.getValidFrom(), entity.getValidTo(), Boolean.TRUE.equals(entity.getIsDefault()));
    return packages.save(entity);
  }

  @Transactional(readOnly = true)
  public List<RateCardPackage> list(UUID vendorPartnerId, UUID vendorOrganisationId, Tenant tenant) {
    if ((vendorPartnerId == null) == (vendorOrganisationId == null)) {
      throw new IllegalArgumentException("Provide exactly one of vendorPartnerId or vendorOrganisationId");
    }
    if (vendorPartnerId != null) {
      VendorPartner link = partners.findById(vendorPartnerId).orElseThrow();
      if (!hasTenant(tenant, link.getPrimaryVendor(), link.getPartnerVendor())) throw new IllegalArgumentException("Not allowed");
      return packages.findByVendorPartner_IdAndIsDeletedFalseOrderByCreatedAtDesc(vendorPartnerId);
    }
    VendorOrganisation link = organisations.findById(vendorOrganisationId).orElseThrow();
    if (!hasTenant(tenant, link.getVendor(), link.getOrganisation())) throw new IllegalArgumentException("Not allowed");
    return packages.findByVendorOrganisation_IdAndIsDeletedFalseOrderByCreatedAtDesc(vendorOrganisationId);
  }

  @Transactional(rollbackFor = Exception.class)
  public RateCardPackage setDefault(UUID packageId, Tenant tenant, UUID actorId) {
    RateCardPackage selected = packages.findByIdAndIsDeletedFalse(packageId)
        .orElseThrow(() -> new IllegalArgumentException("Rate card package not found"));
    if (selected.getVendorPartner() != null) {
      VendorPartner link = selected.getVendorPartner();
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(link.getPrimaryVendor().getId())) {
        throw new IllegalArgumentException("Only the primary vendor can change the default partner package");
      }
      packages.findByVendorPartner_IdAndIsDefaultTrueAndIsDeletedFalse(link.getId())
          .filter(existing -> !existing.getId().equals(selected.getId()))
          .ifPresent(existing -> existing.setIsDefault(false));
      validateDates(selected.getValidFrom(), selected.getValidTo(), true);
      link.setContractStartDate(selected.getValidFrom());
      link.setContractEndDate(selected.getValidTo());
      link.setUpdatedBy(actorId == null ? null : actorId.toString());
    } else {
      VendorOrganisation link = selected.getVendorOrganisation();
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(link.getVendor().getId())) {
        throw new IllegalArgumentException("Only the vendor can change the default organisation package");
      }
      packages.findByVendorOrganisation_IdAndIsDefaultTrueAndIsDeletedFalse(link.getId())
          .filter(existing -> !existing.getId().equals(selected.getId()))
          .ifPresent(existing -> existing.setIsDefault(false));
      validateDates(selected.getValidFrom(), selected.getValidTo(), true);
      link.setContractStartDate(selected.getValidFrom());
      link.setContractEndDate(selected.getValidTo());
      link.setUpdatedBy(actorId == null ? null : actorId.toString());
    }
    selected.setIsDefault(true);
    selected.setUpdatedBy(actorId == null ? null : actorId.toString());
    return packages.save(selected);
  }

  @Transactional(rollbackFor = Exception.class)
  public RateCardPackage update(UUID packageId, RateCardPackageUpdateRequestDTO body, Tenant tenant, UUID actorId) {
    RateCardPackage selected = packages.findByIdAndIsDeletedFalse(packageId)
        .orElseThrow(() -> new IllegalArgumentException("Rate card package not found"));
    VendorPartner partner = selected.getVendorPartner();
    VendorOrganisation organisation = selected.getVendorOrganisation();
    if (partner != null) {
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(partner.getPrimaryVendor().getId())) {
        throw new IllegalArgumentException("Only the primary vendor can update the partner package");
      }
    } else if (tenant == null || tenant.getId() == null || !tenant.getId().equals(organisation.getVendor().getId())) {
      throw new IllegalArgumentException("Only the vendor can update the organisation package");
    }

    if (body.getName() != null) {
      if (body.getName().isBlank()) throw new IllegalArgumentException("Package name cannot be blank");
      selected.setName(body.getName().trim());
    }
    if (body.getValidFrom() != null) selected.setValidFrom(body.getValidFrom());
    if (body.getValidTo() != null) selected.setValidTo(body.getValidTo());
    if (body.getRateCardIds() != null) {
      if (body.getRateCardIds().isEmpty()) throw new IllegalArgumentException("At least one rate card is required");
      if (body.getRateCardIds().stream().distinct().count() != body.getRateCardIds().size()) {
        throw new IllegalArgumentException("Duplicate rateCardIds are not allowed");
      }
      if (partner != null) {
        List<VendorPartnerRateCard> rates = partnerRates.findAllById(body.getRateCardIds());
        if (rates.size() != body.getRateCardIds().size() || rates.stream().anyMatch(r ->
            Boolean.TRUE.equals(r.getIsDeleted()) || !partner.getId().equals(r.getVendorPartner().getId()))) {
          throw new IllegalArgumentException("Every rate card must belong to this vendor partner");
        }
        rates.stream()
            .filter(rate -> !containsRateCard(selected, rate.getId()))
            .forEach(rate -> addPartnerRate(selected, rate));
      } else {
        List<VendorOrganisationRateCard> rates = organisationRates.findAllById(body.getRateCardIds());
        if (rates.size() != body.getRateCardIds().size() || rates.stream().anyMatch(r ->
            Boolean.TRUE.equals(r.getIsDeleted()) || !organisation.getId().equals(r.getVendorOrganisation().getId()))) {
          throw new IllegalArgumentException("Every rate card must belong to this vendor organisation");
        }
        rates.stream()
            .filter(rate -> !containsRateCard(selected, rate.getId()))
            .forEach(rate -> addOrganisationRate(selected, rate));
      }
    }

    if (body.getIsDefault() != null) selected.setIsDefault(body.getIsDefault());
    if (Boolean.TRUE.equals(selected.getIsDefault())) {
      validateDates(selected.getValidFrom(), selected.getValidTo(), true);
      if (partner != null) {
        packages.findByVendorPartner_IdAndIsDefaultTrueAndIsDeletedFalse(partner.getId())
            .filter(existing -> !existing.getId().equals(selected.getId()))
            .ifPresent(existing -> existing.setIsDefault(false));
        partner.setContractStartDate(selected.getValidFrom());
        partner.setContractEndDate(selected.getValidTo());
        partner.setUpdatedBy(actorId == null ? null : actorId.toString());
      } else {
        packages.findByVendorOrganisation_IdAndIsDefaultTrueAndIsDeletedFalse(organisation.getId())
            .filter(existing -> !existing.getId().equals(selected.getId()))
            .ifPresent(existing -> existing.setIsDefault(false));
        organisation.setContractStartDate(selected.getValidFrom());
        organisation.setContractEndDate(selected.getValidTo());
        organisation.setUpdatedBy(actorId == null ? null : actorId.toString());
      }
    }
    validateDates(selected.getValidFrom(), selected.getValidTo(), Boolean.TRUE.equals(selected.getIsDefault()));
    selected.setUpdatedBy(actorId == null ? null : actorId.toString());
    return packages.save(selected);
  }

  @Transactional(rollbackFor = Exception.class)
  public RateCardPackage addRateCards(UUID packageId, List<UUID> rateCardIds, Tenant tenant, UUID actorId) {
    if (rateCardIds == null || rateCardIds.isEmpty()) {
      throw new IllegalArgumentException("At least one rate card is required");
    }
    if (rateCardIds.stream().distinct().count() != rateCardIds.size()) {
      throw new IllegalArgumentException("Duplicate rateCardIds are not allowed");
    }
    RateCardPackage selected = packages.findByIdAndIsDeletedFalse(packageId)
        .orElseThrow(() -> new IllegalArgumentException("Rate card package not found"));
    VendorPartner partner = selected.getVendorPartner();
    VendorOrganisation organisation = selected.getVendorOrganisation();
    if (partner != null) {
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(partner.getPrimaryVendor().getId())) {
        throw new IllegalArgumentException("Only the primary vendor can add cards to the partner package");
      }
      List<VendorPartnerRateCard> rates = partnerRates.findAllById(rateCardIds);
      if (rates.size() != rateCardIds.size() || rates.stream().anyMatch(rate ->
          Boolean.TRUE.equals(rate.getIsDeleted()) || !partner.getId().equals(rate.getVendorPartner().getId()))) {
        throw new IllegalArgumentException("Every rate card must belong to this vendor partner");
      }
      rates.stream().filter(rate -> !containsRateCard(selected, rate.getId()))
          .forEach(rate -> addPartnerRate(selected, rate));
    } else {
      if (tenant == null || tenant.getId() == null || !tenant.getId().equals(organisation.getVendor().getId())) {
        throw new IllegalArgumentException("Only the vendor can add cards to the organisation package");
      }
      List<VendorOrganisationRateCard> rates = organisationRates.findAllById(rateCardIds);
      if (rates.size() != rateCardIds.size() || rates.stream().anyMatch(rate ->
          Boolean.TRUE.equals(rate.getIsDeleted()) || !organisation.getId().equals(rate.getVendorOrganisation().getId()))) {
        throw new IllegalArgumentException("Every rate card must belong to this vendor organisation");
      }
      rates.stream().filter(rate -> !containsRateCard(selected, rate.getId()))
          .forEach(rate -> addOrganisationRate(selected, rate));
    }
    selected.setUpdatedBy(actorId == null ? null : actorId.toString());
    return packages.save(selected);
  }

  private boolean hasTenant(Tenant tenant, Tenant a, Tenant b) {
    return tenant != null && tenant.getId() != null
        && (tenant.getId().equals(a.getId()) || tenant.getId().equals(b.getId()));
  }

  private void validateDates(Long validFrom, Long validTo, boolean required) {
    if (required && (validFrom == null || validTo == null)) {
      throw new IllegalArgumentException("validFrom and validTo are required for a default package");
    }
    if (validFrom != null && validTo != null && validFrom > validTo) {
      throw new IllegalArgumentException("validFrom must be before or equal to validTo");
    }
  }

  private void addPartnerRate(RateCardPackage target, VendorPartnerRateCard rate) {
    RateCardPackageItem item = new RateCardPackageItem();
    item.setRateCardPackage(target);
    item.setVendorPartnerRateCard(rate);
    target.getItems().add(item);
  }

  private void addOrganisationRate(RateCardPackage target, VendorOrganisationRateCard rate) {
    RateCardPackageItem item = new RateCardPackageItem();
    item.setRateCardPackage(target);
    item.setVendorOrganisationRateCard(rate);
    target.getItems().add(item);
  }

  private boolean containsRateCard(RateCardPackage target, UUID rateCardId) {
    return target.getItems().stream().anyMatch(item ->
        item.getVendorPartnerRateCard() != null
            && rateCardId.equals(item.getVendorPartnerRateCard().getId())
        || item.getVendorOrganisationRateCard() != null
            && rateCardId.equals(item.getVendorOrganisationRateCard().getId()));
  }
}

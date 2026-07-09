package com.example.trip_sheet_backend.services.VendorOrganisationRateCardService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationContractApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorOrganisation;
import com.example.trip_sheet_backend.models.VendorOrganisationRateCard;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorOrganisationRepository;

@Service
public class VendorOrganisationRateCardServiceImp implements VendorOrganisationRateCardService {

  private final VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository;
  private final VendorOrganisationRepository vendorOrganisationRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final DutyTypeRepository dutyTypeRepository;

  public VendorOrganisationRateCardServiceImp(
      VendorOrganisationRateCardRepository vendorOrganisationRateCardRepository,
      VendorOrganisationRepository vendorOrganisationRepository,
      VehicleTypeRepository vehicleTypeRepository,
      DutyTypeRepository dutyTypeRepository
  ) {
    this.vendorOrganisationRateCardRepository = vendorOrganisationRateCardRepository;
    this.vendorOrganisationRepository = vendorOrganisationRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.dutyTypeRepository = dutyTypeRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<VendorOrganisationRateCard> createRateCards(
      VendorOrganisationRateCardBulkCreateRequestDTO body,
      Tenant loggedInTenant,
      UUID createdBy
  ) {
    if (body == null || body.getRateCards() == null || body.getRateCards().isEmpty()) {
      throw new RuntimeException("At least one rate card is required");
    }

    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(body.getVendorOrganisationId(), loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can create rate cards");
    }

    List<VendorOrganisationRateCard> existingRateCards = getNonDeletedRateCards(vendorOrganisation.getId());
    if (!existingRateCards.isEmpty() && !isContractExpired(vendorOrganisation)) {
      throw new RuntimeException("Rate cards already exist for this vendor organisation. Update the existing rate card until the current contract expires");
    }

    applySharedVendorOrganisationFields(vendorOrganisation, body);
    vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.PENDING_APPROVAL);
    vendorOrganisation.setActive(false);
    vendorOrganisation.setUpdatedBy(createdBy.toString());
    vendorOrganisationRepository.save(vendorOrganisation);

    return body.getRateCards().stream()
        .map(rateCardRequest -> createSingleRateCard(vendorOrganisation, rateCardRequest, createdBy))
        .toList();
  }

  private VendorOrganisationRateCard createSingleRateCard(
      VendorOrganisation vendorOrganisation,
      VendorOrganisationRateCardCreateRequestDTO body,
      UUID createdBy
  ) {
    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    VendorOrganisationRateCard rateCard = new VendorOrganisationRateCard();
    rateCard.setVendor(vendorOrganisation.getVendor());
    rateCard.setVendorOrganisation(vendorOrganisation);
    applyRateCardFields(
        rateCard,
        vehicleType,
        dutyType,
        body.getCity(),
        body.getBaseFare(),
        body.getExtraKmCharges(),
        body.getExtraHrCharges(),
        body.getDailyAllowanceCharges(),
        body.getEarlyAllowanceCharges(),
        body.getLateAllowanceCharges(),
        body.getSwitchCutOffHrs(),
        body.getSwitchCutOffKms(),
        switchDutyType,
        body.getHourlyAllowance(),
        noShowDutyType,
        body.getNoOfDaysHourCutoff(),
        body.getEarlyAllowanceStartTime(),
        body.getLateAllowanceStartTime(),
        body.getAllowanceCutOffHrs()
    );
    rateCard.setApprovalStatus(VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setCreatedBy(createdBy.toString());

    return vendorOrganisationRateCardRepository.save(rateCard);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisationRateCard updateRateCard(
      UUID rateCardId,
      VendorOrganisationRateCardUpdateRequestDTO body,
      Tenant loggedInTenant,
      UUID updatedBy
  ) {
    VendorOrganisationRateCard rateCard = vendorOrganisationRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation rate card not found"));

    if (Boolean.TRUE.equals(rateCard.getIsDeleted())) {
      throw new RuntimeException("Vendor organisation rate card not found");
    }

    VendorOrganisation vendorOrganisation = rateCard.getVendorOrganisation();
    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can update rate cards");
    }

    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    applyRateCardFields(
        rateCard,
        vehicleType,
        dutyType,
        body.getCity(),
        body.getBaseFare(),
        body.getExtraKmCharges(),
        body.getExtraHrCharges(),
        body.getDailyAllowanceCharges(),
        body.getEarlyAllowanceCharges(),
        body.getLateAllowanceCharges(),
        body.getSwitchCutOffHrs(),
        body.getSwitchCutOffKms(),
        switchDutyType,
        body.getHourlyAllowance(),
        noShowDutyType,
        body.getNoOfDaysHourCutoff(),
        body.getEarlyAllowanceStartTime(),
        body.getLateAllowanceStartTime(),
        body.getAllowanceCutOffHrs()
    );
    rateCard.setApprovalStatus(VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    rateCard.setUpdatedBy(updatedBy.toString());

    VendorOrganisationRateCard savedRateCard = vendorOrganisationRateCardRepository.save(rateCard);

    vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.PENDING_APPROVAL);
    vendorOrganisation.setActive(false);
    vendorOrganisation.setUpdatedBy(updatedBy.toString());
    vendorOrganisationRepository.save(vendorOrganisation);

    return savedRateCard;
  }

  private void applySharedVendorOrganisationFields(
      VendorOrganisation vendorOrganisation,
      VendorOrganisationRateCardBulkCreateRequestDTO body
  ) {
    vendorOrganisation.setPaymentTimelineInDays(body.getPaymentTimelineInDays());
    vendorOrganisation.setLocalBillingStructure(body.getLocalBillingStructure());
    vendorOrganisation.setMinGtgKmLimit(body.getMinGtgKmLimit());
    vendorOrganisation.setMinGtgHrLimit(body.getMinGtgHrLimit());
    vendorOrganisation.setMaxGtgKmLimit(body.getMaxGtgKmLimit());
    vendorOrganisation.setMaxGtgHrLimit(body.getMaxGtgHrLimit());
    vendorOrganisation.setContractStatus(body.getContractStatus());
    vendorOrganisation.setContractStartDate(body.getContractStartDate());
    vendorOrganisation.setContractEndDate(body.getContractEndDate());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisationRateCard reviewRateCard(
      UUID rateCardId,
      VendorOrganisationRateCardApprovalRequestDTO body,
      Tenant loggedInTenant,
      UUID approvedBy
  ) {
    VendorOrganisationRateCard rateCard = vendorOrganisationRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation rate card not found"));

    VendorOrganisation vendorOrganisation = rateCard.getVendorOrganisation();
    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can approve or reject rate cards");
    }

    if (body.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    List<VendorOrganisationRateCard> nonDeletedRateCards = getNonDeletedRateCards(vendorOrganisation.getId());
    Optional<VendorOrganisationRateCard> currentActiveRateCard = findCurrentActiveRateCard(vendorOrganisation, nonDeletedRateCards);

    if (body.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.APPROVED
        && currentActiveRateCard.isPresent()
        && !currentActiveRateCard.get().getId().equals(rateCard.getId())
        && !isContractExpired(vendorOrganisation)) {
      throw new RuntimeException("An active contract already exists for this vendor organisation. Approve a new rate card only after the current contract expires");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    VendorOrganisationRateCard savedRateCard = vendorOrganisationRateCardRepository.save(rateCard);

    if (body.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.APPROVED) {
      vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.ACTIVE);
      vendorOrganisation.setActive(true);
      vendorOrganisation.setUpdatedBy(approvedBy.toString());
      vendorOrganisationRepository.save(vendorOrganisation);
    }

    return savedRateCard;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorOrganisationRateCard> getRateCardsByVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant) {
    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(vendorOrganisationId, loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    return vendorOrganisationRateCardRepository.findByVendorOrganisationIdAndIsDeletedFalse(vendorOrganisation.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public VendorOrganisationRateCard getActiveRateCardByVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant) {
    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(vendorOrganisationId, loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    if (vendorOrganisation.getContractStatus() != VendorOrganisation.ContractStatus.ACTIVE
        || isContractExpired(vendorOrganisation)) {
      return null;
    }

    return findCurrentActiveRateCard(vendorOrganisation, getNonDeletedRateCards(vendorOrganisation.getId()))
        .orElse(null);
  }

  private VendorOrganisation resolveVendorOrganisation(UUID vendorOrganisationId, Tenant loggedInTenant) {
    return vendorOrganisationRepository.findById(vendorOrganisationId)
        .or(() -> findByTenantIdsIfApplicable(vendorOrganisationId, loggedInTenant))
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));
  }

  private java.util.Optional<VendorOrganisation> findByTenantIdsIfApplicable(UUID inputId, Tenant loggedInTenant) {
    if (loggedInTenant == null || loggedInTenant.getTenantType() == null) {
      return java.util.Optional.empty();
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.VENDOR) {
      return vendorOrganisationRepository.findByVendorAndOrganisation_Id(loggedInTenant, inputId);
    }

    if (loggedInTenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      return vendorOrganisationRepository.findByOrganisationAndVendor_Id(loggedInTenant, inputId);
    }

    return java.util.Optional.empty();
  }

  private void validateTenantLinkedToVendorOrganisation(Tenant loggedInTenant, VendorOrganisation vendorOrganisation) {
    if (loggedInTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID loggedInTenantId = loggedInTenant.getId();
    boolean isVendor = vendorOrganisation.getVendor().getId().equals(loggedInTenantId);
    boolean isOrganisation = vendorOrganisation.getOrganisation().getId().equals(loggedInTenantId);

    if (!isVendor && !isOrganisation) {
      throw new RuntimeException("You are not allowed to access this vendor organisation rate card");
    }
  }

  private List<VendorOrganisationRateCard> getNonDeletedRateCards(UUID vendorOrganisationId) {
    return vendorOrganisationRateCardRepository.findByVendorOrganisationIdAndIsDeletedFalse(vendorOrganisationId);
  }

  private Optional<VendorOrganisationRateCard> findCurrentActiveRateCard(
      VendorOrganisation vendorOrganisation,
      List<VendorOrganisationRateCard> rateCards
  ) {
    if (rateCards == null || rateCards.isEmpty() || isContractExpired(vendorOrganisation)) {
      return Optional.empty();
    }

    return rateCards.stream()
        .filter(rateCard -> rateCard.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.APPROVED)
        .max((left, right) -> Long.compare(
            left.getApprovedAt() == null ? 0L : left.getApprovedAt(),
            right.getApprovedAt() == null ? 0L : right.getApprovedAt()
        ));
  }

  private boolean isContractExpired(VendorOrganisation vendorOrganisation) {
    Long contractEndDate = vendorOrganisation.getContractEndDate();
    return contractEndDate != null && contractEndDate < Instant.now().toEpochMilli();
  }

  private DutyType resolveDutyType(UUID dutyTypeId) {
    if (dutyTypeId == null) {
      return null;
    }

    return dutyTypeRepository.findById(dutyTypeId)
        .orElseThrow(() -> new RuntimeException("Duty type not found for id: " + dutyTypeId));
  }

  private void applyRateCardFields(
      VendorOrganisationRateCard rateCard,
      VehicleType vehicleType,
      DutyType dutyType,
      String city,
      java.math.BigDecimal baseFare,
      java.math.BigDecimal extraKmCharges,
      java.math.BigDecimal extraHrCharges,
      java.math.BigDecimal dailyAllowanceCharges,
      java.math.BigDecimal earlyAllowanceCharges,
      java.math.BigDecimal lateAllowanceCharges,
      Integer switchCutOffHrs,
      Integer switchCutOffKms,
      DutyType switchDutyType,
      java.math.BigDecimal hourlyAllowance,
      DutyType noShowDutyType,
      Integer noOfDaysHourCutoff,
      java.time.LocalTime earlyAllowanceStartTime,
      java.time.LocalTime lateAllowanceStartTime,
      Integer allowanceCutOffHrs
  ) {
    rateCard.setVehicleType(vehicleType);
    rateCard.setDutyType(dutyType);
    rateCard.setCity(city.trim());
    rateCard.setBaseFare(baseFare);
    rateCard.setExtraKmCharges(extraKmCharges);
    rateCard.setExtraHrCharges(extraHrCharges);
    rateCard.setDailyAllowanceCharges(dailyAllowanceCharges);
    rateCard.setEarlyAllowanceCharges(earlyAllowanceCharges);
    rateCard.setLateAllowanceCharges(lateAllowanceCharges);
    rateCard.setSwitchCutOffHrs(switchCutOffHrs);
    rateCard.setSwitchCutOffKms(switchCutOffKms);
    rateCard.setSwitchDutyType(switchDutyType);
    rateCard.setHourlyAllowance(hourlyAllowance);
    rateCard.setNoShowDutyType(noShowDutyType);
    rateCard.setNoOfDaysHourCutoff(noOfDaysHourCutoff);
    rateCard.setEarlyAllowanceStartTime(earlyAllowanceStartTime);
    rateCard.setLateAllowanceStartTime(lateAllowanceStartTime);
    rateCard.setAllowanceCutOffHrs(allowanceCutOffHrs);
  }

  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisation updateContractStatus(UUID vendorOrgId, VendorOrganisationContractApprovalRequestDTO body, Tenant loggedInTenant, UUID approvedBy) {

    VendorOrganisation exists = vendorOrganisationRepository.findById(vendorOrgId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found"));

    exists.setContractStatus(body.getContractStatus());
    exists.setUpdatedBy(approvedBy.toString());
    exists.setUpdatedAt(Instant.now().toEpochMilli());

    vendorOrganisationRateCardRepository.findByVendorOrganisationIdAndIsDeletedFalse(vendorOrgId)
        .forEach(rateCard -> {
          rateCard.setApprovalStatus(body.getContractStatus() == VendorOrganisation.ContractStatus.ACTIVE
              ? VendorOrganisationRateCard.ApprovalStatus.APPROVED
              : VendorOrganisationRateCard.ApprovalStatus.REJECTED);
          rateCard.setUpdatedBy(approvedBy.toString());
          rateCard.setUpdatedAt(Instant.now().toEpochMilli());
          vendorOrganisationRateCardRepository.saveAndFlush(rateCard);
        });

    return vendorOrganisationRepository.saveAndFlush(exists);
  }
}

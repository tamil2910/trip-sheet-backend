package com.example.trip_sheet_backend.services.VendorOrganisationRateCardService;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationContractApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorOrganisationRateCardDtos.VendorOrganisationRateCardBulkReviewRequestDTO;
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
    String normalizedCity = normalizeCity(body.getCity());

    validateDuplicateRateCard(
      vendorOrganisation.getId(),
      vehicleType.getId(),
      dutyType.getId(),
      normalizedCity,
      null
    );

    VendorOrganisationRateCard rateCard = new VendorOrganisationRateCard();
    rateCard.setVendor(vendorOrganisation.getVendor());
    rateCard.setVendorOrganisation(vendorOrganisation);
    applyRateCardFields(
        rateCard,
        vehicleType,
        dutyType,
        normalizedCity,
        body.getBaseFare(),
        body.getExtraKmCharges(),
        body.getExtraHrCharges(),
        body.getDailyAllowanceCharges(),
        body.getEarlyAllowanceCharges(),
        body.getLateAllowanceCharges(),
        body.getIsHourlyAllowance(),
        body.getSwitchCutOffHrs(),
        body.getSwitchCutOffKms(),
        switchDutyType,
        body.getHourlyAllowance(),
        noShowDutyType,
        body.getNoOfDaysHourCutoff(),
        body.getEarlyAllowanceEndTime(),
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
    String normalizedCity = normalizeCity(body.getCity());

    validateDuplicateRateCard(
      vendorOrganisation.getId(),
      vehicleType.getId(),
      dutyType.getId(),
      normalizedCity,
      rateCard.getId()
    );

    applyRateCardFields(
        rateCard,
        vehicleType,
        dutyType,
      normalizedCity,
        body.getBaseFare(),
        body.getExtraKmCharges(),
        body.getExtraHrCharges(),
        body.getDailyAllowanceCharges(),
        body.getEarlyAllowanceCharges(),
        body.getLateAllowanceCharges(),
        body.getIsHourlyAllowance(),
        body.getSwitchCutOffHrs(),
        body.getSwitchCutOffKms(),
        switchDutyType,
        body.getHourlyAllowance(),
        noShowDutyType,
        body.getNoOfDaysHourCutoff(),
        body.getEarlyAllowanceEndTime(),
        body.getLateAllowanceStartTime(),
        body.getAllowanceCutOffHrs()
    );
    rateCard.setApprovalStatus(VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    rateCard.setUpdatedBy(updatedBy.toString());

    VendorOrganisationRateCard savedRateCard = vendorOrganisationRateCardRepository.save(rateCard);

    syncVendorOrganisationContractStatus(vendorOrganisation, updatedBy);

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

    if (vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("The vendor who raised the latest changes cannot approve or reject them");
    }

    if (body.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    VendorOrganisationRateCard savedRateCard = vendorOrganisationRateCardRepository.save(rateCard);

    syncVendorOrganisationContractStatus(vendorOrganisation, approvedBy);

    return savedRateCard;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorOrganisation bulkReviewRateCards(
      UUID vendorOrganisationId,
      VendorOrganisationRateCardBulkReviewRequestDTO body,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    if (body == null || body.getRateCards() == null || body.getRateCards().isEmpty()) {
      throw new RuntimeException("At least one rate card action is required");
    }

    VendorOrganisation vendorOrganisation = resolveVendorOrganisation(vendorOrganisationId, loggedInTenant);

    validateTenantLinkedToVendorOrganisation(loggedInTenant, vendorOrganisation);

    Set<UUID> requestedRateCardIds = body.getRateCards().stream()
        .filter(actionItem -> actionItem.getAction() != null)
        .filter(actionItem -> actionItem.getAction() != VendorOrganisationRateCardBulkReviewRequestDTO.BulkAction.CREATE)
        .map(VendorOrganisationRateCardBulkReviewRequestDTO.RateCardActionDTO::getRateCardId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());

    Map<UUID, VendorOrganisationRateCard> rateCardById = vendorOrganisationRateCardRepository.findAllById(requestedRateCardIds)
        .stream()
        .collect(Collectors.toMap(VendorOrganisationRateCard::getId, Function.identity()));

    for (VendorOrganisationRateCardBulkReviewRequestDTO.RateCardActionDTO actionItem : body.getRateCards()) {
      if (actionItem.getAction() == null) {
        throw new RuntimeException("Action is required for each rate card item");
      }

      if (actionItem.getAction() == VendorOrganisationRateCardBulkReviewRequestDTO.BulkAction.CREATE) {
        applyBulkRateCardCreate(vendorOrganisation, actionItem.getNewRateCard(), loggedInTenant, actedBy);
        continue;
      }

      if (actionItem.getRateCardId() == null) {
        throw new RuntimeException("Rate card id is required for " + actionItem.getAction() + " action");
      }

      VendorOrganisationRateCard rateCard = rateCardById.get(actionItem.getRateCardId());
      if (rateCard == null || Boolean.TRUE.equals(rateCard.getIsDeleted())) {
        throw new RuntimeException("Vendor organisation rate card not found for id: " + actionItem.getRateCardId());
      }

      if (!vendorOrganisation.getId().equals(rateCard.getVendorOrganisation().getId())) {
        throw new RuntimeException("Rate card does not belong to the requested vendor organisation");
      }

      switch (actionItem.getAction()) {
        case APPROVE -> applyBulkApprovalDecision(rateCard, VendorOrganisationRateCard.ApprovalStatus.APPROVED, loggedInTenant, actedBy);
        case REJECT -> applyBulkApprovalDecision(rateCard, VendorOrganisationRateCard.ApprovalStatus.REJECTED, loggedInTenant, actedBy);
        case UPDATE -> applyPartialRateCardUpdate(rateCard, actionItem.getChanges(), loggedInTenant, actedBy);
        case CREATE -> throw new RuntimeException("Unsupported bulk action");
        default -> throw new RuntimeException("Unsupported bulk action");
      }
    }

    syncVendorOrganisationContractStatus(vendorOrganisation, actedBy);

    return vendorOrganisationRepository.findById(vendorOrganisationId)
        .orElseThrow(() -> new RuntimeException("Vendor organisation relationship not found after bulk review"));
  }

  private void applyBulkRateCardCreate(
      VendorOrganisation vendorOrganisation,
      VendorOrganisationRateCardCreateRequestDTO newRateCard,
      Tenant loggedInTenant,
      UUID createdBy
  ) {
    if (newRateCard == null) {
      throw new RuntimeException("New rate card payload is required for CREATE action");
    }

    if (!vendorOrganisation.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can create rate cards");
    }

    createSingleRateCard(vendorOrganisation, newRateCard, createdBy);
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

  private void applyBulkApprovalDecision(
      VendorOrganisationRateCard rateCard,
      VendorOrganisationRateCard.ApprovalStatus approvalStatus,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    if (rateCard.getVendor() != null && rateCard.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("The vendor who raised the latest changes cannot approve or reject them");
    }

    rateCard.setApprovalStatus(approvalStatus);
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(actedBy.toString());
    rateCard.setUpdatedBy(actedBy.toString());
    vendorOrganisationRateCardRepository.save(rateCard);
  }

  private void applyPartialRateCardUpdate(
      VendorOrganisationRateCard rateCard,
      VendorOrganisationRateCardBulkReviewRequestDTO.PartialUpdateDTO changes,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    if (changes == null) {
      throw new RuntimeException("Changes payload is required for UPDATE action");
    }

    if (!rateCard.getVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only vendor can update rate cards");
    }

    VehicleType vehicleType = changes.getVehicleTypeId() == null
        ? rateCard.getVehicleType()
        : vehicleTypeRepository.findById(changes.getVehicleTypeId())
            .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = changes.getDutyTypeId() == null
        ? rateCard.getDutyType()
        : dutyTypeRepository.findById(changes.getDutyTypeId())
            .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = changes.getSwitchDutyTypeId() == null
        ? rateCard.getSwitchDutyType()
        : resolveDutyType(changes.getSwitchDutyTypeId());

    DutyType noShowDutyType = changes.getNoShowDutyTypeId() == null
        ? rateCard.getNoShowDutyType()
        : resolveDutyType(changes.getNoShowDutyTypeId());

    String normalizedCity = changes.getCity() == null
        ? rateCard.getCity()
        : normalizeCity(changes.getCity());

    validateDuplicateRateCard(
        rateCard.getVendorOrganisation().getId(),
        vehicleType.getId(),
        dutyType.getId(),
        normalizedCity,
        rateCard.getId()
    );

    applyRateCardFields(
        rateCard,
        vehicleType,
        dutyType,
        normalizedCity,
        firstNonNull(changes.getBaseFare(), rateCard.getBaseFare()),
        firstNonNull(changes.getExtraKmCharges(), rateCard.getExtraKmCharges()),
        firstNonNull(changes.getExtraHrCharges(), rateCard.getExtraHrCharges()),
        firstNonNull(changes.getDailyAllowanceCharges(), rateCard.getDailyAllowanceCharges()),
        firstNonNull(changes.getEarlyAllowanceCharges(), rateCard.getEarlyAllowanceCharges()),
        firstNonNull(changes.getLateAllowanceCharges(), rateCard.getLateAllowanceCharges()),
        firstNonNull(changes.getIsHourlyAllowance(), rateCard.getIsHourlyAllowance()),
        firstNonNull(changes.getSwitchCutOffHrs(), rateCard.getSwitchCutOffHrs()),
        firstNonNull(changes.getSwitchCutOffKms(), rateCard.getSwitchCutOffKms()),
        switchDutyType,
        firstNonNull(changes.getHourlyAllowance(), rateCard.getHourlyAllowance()),
        noShowDutyType,
        firstNonNull(changes.getNoOfDaysHourCutoff(), rateCard.getNoOfDaysHourCutoff()),
        firstNonNull(changes.getEarlyAllowanceEndTime(), rateCard.getEarlyAllowanceEndTime()),
        firstNonNull(changes.getLateAllowanceStartTime(), rateCard.getLateAllowanceStartTime()),
        firstNonNull(changes.getAllowanceCutOffHrs(), rateCard.getAllowanceCutOffHrs())
    );

    rateCard.setApprovalStatus(VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    rateCard.setUpdatedBy(actedBy.toString());
    vendorOrganisationRateCardRepository.save(rateCard);
  }

  private <T> T firstNonNull(T updatedValue, T existingValue) {
    return updatedValue != null ? updatedValue : existingValue;
  }

  private void syncVendorOrganisationContractStatus(VendorOrganisation vendorOrganisation, UUID updatedBy) {
    List<VendorOrganisationRateCard> nonDeletedRateCards = getNonDeletedRateCards(vendorOrganisation.getId());

    boolean hasApproved = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.APPROVED);
    boolean hasPending = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.PENDING_APPROVAL);
    boolean hasRejected = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorOrganisationRateCard.ApprovalStatus.REJECTED);

    if (nonDeletedRateCards.isEmpty()) {
      vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.TERMINATED);
      vendorOrganisation.setActive(false);
    } else if (hasApproved) {
      vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.ACTIVE);
      vendorOrganisation.setActive(true);
    } else if (hasPending) {
      vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.PENDING_APPROVAL);
      vendorOrganisation.setActive(false);
    } else if (hasRejected) {
      vendorOrganisation.setContractStatus(VendorOrganisation.ContractStatus.REJECTED);
      vendorOrganisation.setActive(false);
    }

    vendorOrganisation.setUpdatedBy(updatedBy.toString());
    vendorOrganisationRepository.save(vendorOrganisation);
  }

  private void validateDuplicateRateCard(
      UUID vendorOrganisationId,
      UUID vehicleTypeId,
      UUID dutyTypeId,
      String normalizedCity,
      UUID excludingRateCardId
  ) {
    boolean duplicateExists = excludingRateCardId == null
        ? vendorOrganisationRateCardRepository.existsByVendorOrganisationIdAndVehicleTypeIdAndDutyTypeIdAndCityIgnoreCaseAndIsDeletedFalse(
            vendorOrganisationId,
            vehicleTypeId,
            dutyTypeId,
            normalizedCity
        )
        : vendorOrganisationRateCardRepository.existsByVendorOrganisationIdAndVehicleTypeIdAndDutyTypeIdAndCityIgnoreCaseAndIsDeletedFalseAndIdNot(
            vendorOrganisationId,
            vehicleTypeId,
            dutyTypeId,
            normalizedCity,
            excludingRateCardId
        );

    if (duplicateExists) {
      throw new RuntimeException("Duplicate rate card is not allowed for the same vendor organisation, vehicle type, duty type and city");
    }
  }

  private String normalizeCity(String city) {
    if (city == null || city.trim().isEmpty()) {
      throw new RuntimeException("City is required");
    }

    return city.trim().toLowerCase(Locale.ROOT);
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
      BigDecimal baseFare,
      BigDecimal extraKmCharges,
      BigDecimal extraHrCharges,
      BigDecimal dailyAllowanceCharges,
      BigDecimal earlyAllowanceCharges,
      BigDecimal lateAllowanceCharges,
      Boolean isHourlyAllowance,
      Integer switchCutOffHrs,
      Integer switchCutOffKms,
      DutyType switchDutyType,
      BigDecimal hourlyAllowance,
      DutyType noShowDutyType,
      Integer noOfDaysHourCutoff,
      LocalTime earlyAllowanceEndTime,
      LocalTime lateAllowanceStartTime,
      Integer allowanceCutOffHrs
  ) {
    rateCard.setVehicleType(vehicleType);
    rateCard.setDutyType(dutyType);
    rateCard.setCity(city);
    rateCard.setBaseFare(baseFare);
    rateCard.setExtraKmCharges(extraKmCharges);
    rateCard.setExtraHrCharges(extraHrCharges);
    rateCard.setDailyAllowanceCharges(dailyAllowanceCharges);
    rateCard.setEarlyAllowanceCharges(earlyAllowanceCharges);
    rateCard.setLateAllowanceCharges(lateAllowanceCharges);
    rateCard.setIsHourlyAllowance(isHourlyAllowance);
    rateCard.setSwitchCutOffHrs(switchCutOffHrs);
    rateCard.setSwitchCutOffKms(switchCutOffKms);
    rateCard.setSwitchDutyType(switchDutyType);
    rateCard.setHourlyAllowance(hourlyAllowance);
    rateCard.setNoShowDutyType(noShowDutyType);
    rateCard.setNoOfDaysHourCutoff(noOfDaysHourCutoff);
    rateCard.setEarlyAllowanceEndTime(earlyAllowanceEndTime);
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

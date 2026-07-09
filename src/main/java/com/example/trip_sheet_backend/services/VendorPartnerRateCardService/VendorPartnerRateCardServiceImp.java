package com.example.trip_sheet_backend.services.VendorPartnerRateCardService;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkReviewRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardCreateRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardUpdateRequestDTO;
import com.example.trip_sheet_backend.models.DutyType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.models.VendorPartnerRateCard;
import com.example.trip_sheet_backend.repositories.DutyTypeRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRateCardRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

@Service
public class VendorPartnerRateCardServiceImp implements VendorPartnerRateCardService {

  private final VendorPartnerRateCardRepository vendorPartnerRateCardRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final DutyTypeRepository dutyTypeRepository;

  public VendorPartnerRateCardServiceImp(
      VendorPartnerRateCardRepository vendorPartnerRateCardRepository,
      VendorPartnerRepository vendorPartnerRepository,
      VehicleTypeRepository vehicleTypeRepository,
      DutyTypeRepository dutyTypeRepository
  ) {
    this.vendorPartnerRateCardRepository = vendorPartnerRateCardRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.dutyTypeRepository = dutyTypeRepository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public List<VendorPartnerRateCard> createRateCards(
      VendorPartnerRateCardBulkCreateRequestDTO body,
      Tenant loggedInTenant,
      UUID createdBy
  ) {
    if (body == null || body.getRateCards() == null || body.getRateCards().isEmpty()) {
      throw new RuntimeException("At least one rate card is required");
    }

    VendorPartner vendorPartner = vendorPartnerRepository.findById(body.getVendorPartnerId())
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    List<VendorPartnerRateCard> existingRateCards = getNonDeletedRateCards(vendorPartner.getId());
    if (!existingRateCards.isEmpty()) {
      throw new RuntimeException("Rate cards already exist for this vendor partner. Use update flow to modify and re-submit for approval");
    }

    applySharedVendorPartnerFields(vendorPartner, body);
    vendorPartner.setContractStatus(VendorPartner.ContractStatus.PENDING_APPROVAL);
    vendorPartner.setUpdatedBy(createdBy.toString());
    vendorPartnerRepository.save(vendorPartner);

    return body.getRateCards().stream()
        .map(rateCardRequest -> createSingleRateCard(vendorPartner, rateCardRequest, loggedInTenant, createdBy))
        .toList();
  }

  private VendorPartnerRateCard createSingleRateCard(
      VendorPartner vendorPartner,
      VendorPartnerRateCardCreateRequestDTO body,
      Tenant raisedByVendor,
      UUID createdBy
  ) {
    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    String normalizedCity = normalizeCity(body.getCity());
    validateDuplicateRateCard(vendorPartner.getId(), vehicleType.getId(), dutyType.getId(), normalizedCity, null);

    VendorPartnerRateCard rateCard = new VendorPartnerRateCard();
    rateCard.setPrimaryVendor(vendorPartner.getPrimaryVendor());
    rateCard.setRaisedByVendor(raisedByVendor);
    rateCard.setVendorPartner(vendorPartner);
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
    rateCard.setApprovalStatus(VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setCreatedBy(createdBy.toString());

    return vendorPartnerRateCardRepository.save(rateCard);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartnerRateCard updateRateCard(
      UUID rateCardId,
      VendorPartnerRateCardUpdateRequestDTO body,
      Tenant loggedInTenant,
      UUID updatedBy
  ) {
    VendorPartnerRateCard rateCard = vendorPartnerRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor partner rate card not found"));

    if (Boolean.TRUE.equals(rateCard.getIsDeleted())) {
      throw new RuntimeException("Vendor partner rate card not found");
    }

    VendorPartner vendorPartner = rateCard.getVendorPartner();
    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    String normalizedCity = normalizeCity(body.getCity());
    validateDuplicateRateCard(vendorPartner.getId(), vehicleType.getId(), dutyType.getId(), normalizedCity, rateCard.getId());

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
    rateCard.setApprovalStatus(VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    // The vendor who edits the card becomes the current requester for the next approval step.
    rateCard.setRaisedByVendor(loggedInTenant);
    rateCard.setUpdatedBy(updatedBy.toString());

    VendorPartnerRateCard savedRateCard = vendorPartnerRateCardRepository.save(rateCard);

    syncVendorPartnerContractStatus(vendorPartner, updatedBy);

    return savedRateCard;
  }

  private void applySharedVendorPartnerFields(
      VendorPartner vendorPartner,
      VendorPartnerRateCardBulkCreateRequestDTO body
  ) {
    vendorPartner.setPaymentTimelineInDays(body.getPaymentTimelineInDays());
    vendorPartner.setLocalBillingStructure(body.getLocalBillingStructure());
    vendorPartner.setMinGtgKmLimit(body.getMinGtgKmLimit());
    vendorPartner.setMinGtgHrLimit(body.getMinGtgHrLimit());
    vendorPartner.setMaxGtgKmLimit(body.getMaxGtgKmLimit());
    vendorPartner.setMaxGtgHrLimit(body.getMaxGtgHrLimit());
    vendorPartner.setContractStatus(body.getContractStatus());
    vendorPartner.setContractStartDate(body.getContractStartDate());
    vendorPartner.setContractEndDate(body.getContractEndDate());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartnerRateCard reviewRateCard(
      UUID rateCardId,
      VendorPartnerRateCardApprovalRequestDTO body,
      Tenant loggedInTenant,
      UUID approvedBy
  ) {
    VendorPartnerRateCard rateCard = vendorPartnerRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor partner rate card not found"));

    VendorPartner vendorPartner = rateCard.getVendorPartner();
    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    Tenant currentRaisedByVendor = rateCard.getRaisedByVendor() != null
        ? rateCard.getRaisedByVendor()
        : rateCard.getPrimaryVendor();

    if (currentRaisedByVendor != null && currentRaisedByVendor.getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("The vendor who raised the latest changes cannot approve or reject them");
    }

    if (body.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    VendorPartnerRateCard savedRateCard = vendorPartnerRateCardRepository.save(rateCard);

    syncVendorPartnerContractStatus(vendorPartner, approvedBy);

    return savedRateCard;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartner deleteRateCard(UUID rateCardId, Tenant loggedInTenant, UUID deletedBy) {
    VendorPartnerRateCard rateCard = vendorPartnerRateCardRepository.findById(rateCardId)
        .orElseThrow(() -> new RuntimeException("Vendor partner rate card not found"));

    if (Boolean.TRUE.equals(rateCard.getIsDeleted())) {
      throw new RuntimeException("Vendor partner rate card not found");
    }

    VendorPartner vendorPartner = rateCard.getVendorPartner();
    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    rateCard.setIsDeleted(true);
    rateCard.setDeletedAt(Instant.now().toEpochMilli());
    rateCard.setDeletedBy(deletedBy.toString());
    rateCard.setUpdatedBy(deletedBy.toString());
    vendorPartnerRateCardRepository.save(rateCard);

    syncVendorPartnerContractStatus(vendorPartner, deletedBy);

    return vendorPartnerRepository.findById(vendorPartner.getId())
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found after rate card deletion"));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public VendorPartner bulkReviewRateCards(
      UUID vendorPartnerId,
      VendorPartnerRateCardBulkReviewRequestDTO body,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    if (body == null || body.getRateCards() == null || body.getRateCards().isEmpty()) {
      throw new RuntimeException("At least one rate card action is required");
    }

    VendorPartner vendorPartner = vendorPartnerRepository.findById(vendorPartnerId)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    Set<UUID> requestedRateCardIds = body.getRateCards().stream()
        .map(VendorPartnerRateCardBulkReviewRequestDTO.RateCardActionDTO::getRateCardId)
        .collect(Collectors.toSet());

    Map<UUID, VendorPartnerRateCard> rateCardById = vendorPartnerRateCardRepository.findAllById(requestedRateCardIds)
        .stream()
        .collect(Collectors.toMap(VendorPartnerRateCard::getId, Function.identity()));

    for (VendorPartnerRateCardBulkReviewRequestDTO.RateCardActionDTO actionItem : body.getRateCards()) {
      VendorPartnerRateCard rateCard = rateCardById.get(actionItem.getRateCardId());
      if (rateCard == null || Boolean.TRUE.equals(rateCard.getIsDeleted())) {
        throw new RuntimeException("Vendor partner rate card not found for id: " + actionItem.getRateCardId());
      }

      if (!vendorPartner.getId().equals(rateCard.getVendorPartner().getId())) {
        throw new RuntimeException("Rate card does not belong to the requested vendor partner");
      }

      switch (actionItem.getAction()) {
        case APPROVE -> applyBulkApprovalDecision(rateCard, VendorPartnerRateCard.ApprovalStatus.APPROVED, loggedInTenant, actedBy);
        case REJECT -> applyBulkApprovalDecision(rateCard, VendorPartnerRateCard.ApprovalStatus.REJECTED, loggedInTenant, actedBy);
        case UPDATE -> applyPartialRateCardUpdate(rateCard, actionItem.getChanges(), loggedInTenant, actedBy);
        default -> throw new RuntimeException("Unsupported bulk action");
      }
    }

    syncVendorPartnerContractStatus(vendorPartner, actedBy);

    return vendorPartnerRepository.findById(vendorPartnerId)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found after bulk review"));
  }

  @Override
  @Transactional(readOnly = true)
  public List<VendorPartnerRateCard> getRateCardsByVendorPartner(UUID vendorPartnerId, Tenant loggedInTenant) {
    VendorPartner vendorPartner = vendorPartnerRepository.findById(vendorPartnerId)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    return vendorPartnerRateCardRepository.findByVendorPartnerIdAndIsDeletedFalse(vendorPartnerId);
  }

  @Override
  @Transactional(readOnly = true)
  public VendorPartnerRateCard getActiveRateCardByVendorPartner(UUID vendorPartnerId, Tenant loggedInTenant) {
    VendorPartner vendorPartner = vendorPartnerRepository.findById(vendorPartnerId)
        .orElseThrow(() -> new RuntimeException("Vendor partner relationship not found"));

    validateTenantLinkedToVendorPartner(loggedInTenant, vendorPartner);

    if (vendorPartner.getContractStatus() != VendorPartner.ContractStatus.ACTIVE || isContractExpired(vendorPartner)) {
      return null;
    }

    List<VendorPartnerRateCard> nonDeletedRateCards = getNonDeletedRateCards(vendorPartnerId);

    return findCurrentActiveRateCard(vendorPartner, nonDeletedRateCards)
        .orElse(null);
  }

  private void validateTenantLinkedToVendorPartner(Tenant loggedInTenant, VendorPartner vendorPartner) {
    if (loggedInTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    UUID loggedInTenantId = loggedInTenant.getId();
    boolean isPrimaryVendor = vendorPartner.getPrimaryVendor().getId().equals(loggedInTenantId);
    boolean isPartnerVendor = vendorPartner.getPartnerVendor().getId().equals(loggedInTenantId);

    if (!isPrimaryVendor && !isPartnerVendor) {
      throw new RuntimeException("You are not allowed to access this vendor partner rate card");
    }
  }

  private List<VendorPartnerRateCard> getNonDeletedRateCards(UUID vendorPartnerId) {
    return vendorPartnerRateCardRepository.findByVendorPartnerIdAndIsDeletedFalse(vendorPartnerId);
  }

  private Optional<VendorPartnerRateCard> findCurrentActiveRateCard(
      VendorPartner vendorPartner,
      List<VendorPartnerRateCard> rateCards
  ) {
    if (rateCards == null || rateCards.isEmpty()) {
      return Optional.empty();
    }

    List<VendorPartnerRateCard> approvedRateCards = rateCards.stream()
        .filter(rateCard -> rateCard.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.APPROVED)
        .toList();

    if (approvedRateCards.isEmpty()) {
      return Optional.empty();
    }

    if (isContractExpired(vendorPartner)) {
      return Optional.empty();
    }

    return approvedRateCards.stream()
        .max((first, second) -> Long.compare(
            getRateCardPriorityTimestamp(first),
            getRateCardPriorityTimestamp(second)
        ));
  }

  private long getRateCardPriorityTimestamp(VendorPartnerRateCard rateCard) {
    if (rateCard.getApprovedAt() != null) {
      return rateCard.getApprovedAt();
    }

    if (rateCard.getUpdatedAt() != null) {
      return rateCard.getUpdatedAt();
    }

    if (rateCard.getCreatedAt() != null) {
      return rateCard.getCreatedAt();
    }

    return 0L;
  }

  private boolean isContractExpired(VendorPartner vendorPartner) {
    Long contractEndDate = vendorPartner.getContractEndDate();
    return contractEndDate != null && contractEndDate < Instant.now().toEpochMilli();
  }

  private DutyType resolveDutyType(UUID dutyTypeId) {
    if (dutyTypeId == null) {
      return null;
    }

    return dutyTypeRepository.findById(dutyTypeId)
        .orElseThrow(() -> new RuntimeException("Duty type not found for id: " + dutyTypeId));
  }

  private void applyBulkApprovalDecision(
      VendorPartnerRateCard rateCard,
      VendorPartnerRateCard.ApprovalStatus approvalStatus,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    Tenant currentRaisedByVendor = rateCard.getRaisedByVendor() != null
        ? rateCard.getRaisedByVendor()
        : rateCard.getPrimaryVendor();

    if (currentRaisedByVendor != null && currentRaisedByVendor.getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("The vendor who raised the latest changes cannot approve or reject them");
    }

    rateCard.setApprovalStatus(approvalStatus);
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(actedBy.toString());
    rateCard.setUpdatedBy(actedBy.toString());
    vendorPartnerRateCardRepository.save(rateCard);
  }

  private void applyPartialRateCardUpdate(
      VendorPartnerRateCard rateCard,
      VendorPartnerRateCardBulkReviewRequestDTO.PartialUpdateDTO changes,
      Tenant loggedInTenant,
      UUID actedBy
  ) {
    if (changes == null) {
      throw new RuntimeException("Changes payload is required for UPDATE action");
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
        rateCard.getVendorPartner().getId(),
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
        firstNonNull(changes.getSwitchCutOffHrs(), rateCard.getSwitchCutOffHrs()),
        firstNonNull(changes.getSwitchCutOffKms(), rateCard.getSwitchCutOffKms()),
        switchDutyType,
        firstNonNull(changes.getHourlyAllowance(), rateCard.getHourlyAllowance()),
        noShowDutyType,
        firstNonNull(changes.getNoOfDaysHourCutoff(), rateCard.getNoOfDaysHourCutoff()),
        firstNonNull(changes.getEarlyAllowanceStartTime(), rateCard.getEarlyAllowanceStartTime()),
        firstNonNull(changes.getLateAllowanceStartTime(), rateCard.getLateAllowanceStartTime()),
        firstNonNull(changes.getAllowanceCutOffHrs(), rateCard.getAllowanceCutOffHrs())
    );

    rateCard.setApprovalStatus(VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    rateCard.setRaisedByVendor(loggedInTenant);
    rateCard.setUpdatedBy(actedBy.toString());
    vendorPartnerRateCardRepository.save(rateCard);
  }

  private <T> T firstNonNull(T updatedValue, T existingValue) {
    return updatedValue != null ? updatedValue : existingValue;
  }

  private void syncVendorPartnerContractStatus(VendorPartner vendorPartner, UUID updatedBy) {
    List<VendorPartnerRateCard> nonDeletedRateCards = getNonDeletedRateCards(vendorPartner.getId());

    boolean hasApproved = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.APPROVED);
    boolean hasPending = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    boolean hasRejected = nonDeletedRateCards.stream()
        .anyMatch(rateCard -> rateCard.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.REJECTED);

    if (nonDeletedRateCards.isEmpty()) {
      vendorPartner.setContractStatus(VendorPartner.ContractStatus.TERMINATED);
    } else if (hasApproved) {
      vendorPartner.setContractStatus(VendorPartner.ContractStatus.ACTIVE);
    } else if (hasPending) {
      vendorPartner.setContractStatus(VendorPartner.ContractStatus.PENDING_APPROVAL);
    } else if (hasRejected) {
      vendorPartner.setContractStatus(VendorPartner.ContractStatus.REJECTED);
    }

    vendorPartner.setUpdatedBy(updatedBy.toString());
    vendorPartnerRepository.save(vendorPartner);
  }

  private void validateDuplicateRateCard(
      UUID vendorPartnerId,
      UUID vehicleTypeId,
      UUID dutyTypeId,
      String normalizedCity,
      UUID excludingRateCardId
  ) {
    boolean duplicateExists = excludingRateCardId == null
        ? vendorPartnerRateCardRepository.existsByVendorPartnerIdAndVehicleTypeIdAndDutyTypeIdAndCityIgnoreCaseAndIsDeletedFalse(
            vendorPartnerId,
            vehicleTypeId,
            dutyTypeId,
            normalizedCity
        )
        : vendorPartnerRateCardRepository.existsByVendorPartnerIdAndVehicleTypeIdAndDutyTypeIdAndCityIgnoreCaseAndIsDeletedFalseAndIdNot(
            vendorPartnerId,
            vehicleTypeId,
            dutyTypeId,
            normalizedCity,
            excludingRateCardId
        );

    if (duplicateExists) {
      throw new RuntimeException("Duplicate rate card is not allowed for the same vendor partner, vehicle type, duty type and city");
    }
  }

  private String normalizeCity(String city) {
    if (city == null || city.trim().isEmpty()) {
      throw new RuntimeException("City is required");
    }

    return city.trim().toLowerCase(Locale.ROOT);
  }

  private void applyRateCardFields(
      VendorPartnerRateCard rateCard,
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
    rateCard.setCity(city);
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
}

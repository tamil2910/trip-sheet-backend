package com.example.trip_sheet_backend.services.VendorPartnerRateCardService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardApprovalRequestDTO;
import com.example.trip_sheet_backend.dtos.VendorPartnerRateCardDtos.VendorPartnerRateCardBulkCreateRequestDTO;
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

    if (!vendorPartner.getPrimaryVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only primary vendor can create rate cards");
    }

    List<VendorPartnerRateCard> existingRateCards = getNonDeletedRateCards(vendorPartner.getId());
    if (!existingRateCards.isEmpty() && !isContractExpired(vendorPartner)) {
      throw new RuntimeException("Rate cards already exist for this vendor partner. Update the existing rate card until the current contract expires");
    }

    applySharedVendorPartnerFields(vendorPartner, body);
    vendorPartner.setContractStatus(VendorPartner.ContractStatus.PENDING_APPROVAL);
    vendorPartner.setVendorPartnerRateCardId(null);
    vendorPartner.setUpdatedBy(createdBy.toString());
    vendorPartnerRepository.save(vendorPartner);

    return body.getRateCards().stream()
        .map(rateCardRequest -> createSingleRateCard(vendorPartner, rateCardRequest, createdBy))
        .toList();
  }

  private VendorPartnerRateCard createSingleRateCard(
      VendorPartner vendorPartner,
      VendorPartnerRateCardCreateRequestDTO body,
      UUID createdBy
  ) {
    VehicleType vehicleType = vehicleTypeRepository.findById(body.getVehicleTypeId())
        .orElseThrow(() -> new RuntimeException("Vehicle type not found"));

    DutyType dutyType = dutyTypeRepository.findById(body.getDutyTypeId())
        .orElseThrow(() -> new RuntimeException("Duty type not found"));

    DutyType switchDutyType = resolveDutyType(body.getSwitchDutyTypeId());
    DutyType noShowDutyType = resolveDutyType(body.getNoShowDutyTypeId());

    VendorPartnerRateCard rateCard = new VendorPartnerRateCard();
    rateCard.setPrimaryVendor(vendorPartner.getPrimaryVendor());
    rateCard.setVendorPartner(vendorPartner);
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

    if (!vendorPartner.getPrimaryVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only primary vendor can update rate cards");
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
    rateCard.setApprovalStatus(VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL);
    rateCard.setApprovedAt(null);
    rateCard.setApprovedBy(null);
    rateCard.setUpdatedBy(updatedBy.toString());

    VendorPartnerRateCard savedRateCard = vendorPartnerRateCardRepository.save(rateCard);

    vendorPartner.setContractStatus(VendorPartner.ContractStatus.PENDING_APPROVAL);
    if (Objects.equals(vendorPartner.getVendorPartnerRateCardId(), rateCard.getId())) {
      vendorPartner.setVendorPartnerRateCardId(null);
    }
    vendorPartner.setUpdatedBy(updatedBy.toString());
    vendorPartnerRepository.save(vendorPartner);

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

    if (!vendorPartner.getPrimaryVendor().getId().equals(loggedInTenant.getId())) {
      throw new RuntimeException("Only primary vendor can approve or reject rate cards");
    }

    if (body.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.PENDING_APPROVAL) {
      throw new RuntimeException("Invalid approval status");
    }

    List<VendorPartnerRateCard> nonDeletedRateCards = getNonDeletedRateCards(vendorPartner.getId());
    Optional<VendorPartnerRateCard> currentActiveRateCard = findCurrentActiveRateCard(vendorPartner, nonDeletedRateCards);

    if (body.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.APPROVED
        && currentActiveRateCard.isPresent()
        && !currentActiveRateCard.get().getId().equals(rateCard.getId())
        && !isContractExpired(vendorPartner)) {
      throw new RuntimeException("An active contract already exists for this vendor partner. Approve a new rate card only after the current contract expires");
    }

    rateCard.setApprovalStatus(body.getApprovalStatus());
    rateCard.setApprovedAt(Instant.now().toEpochMilli());
    rateCard.setApprovedBy(approvedBy.toString());
    rateCard.setUpdatedBy(approvedBy.toString());

    VendorPartnerRateCard savedRateCard = vendorPartnerRateCardRepository.save(rateCard);

    if (body.getApprovalStatus() == VendorPartnerRateCard.ApprovalStatus.APPROVED) {
      vendorPartner.setVendorPartnerRateCardId(savedRateCard.getId());
      vendorPartner.setContractStatus(VendorPartner.ContractStatus.ACTIVE);
      vendorPartner.setUpdatedBy(approvedBy.toString());
      vendorPartnerRepository.save(vendorPartner);
    }

    return savedRateCard;
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

    if (vendorPartner.getVendorPartnerRateCardId() != null) {
      Optional<VendorPartnerRateCard> configuredRateCard = approvedRateCards.stream()
          .filter(rateCard -> Objects.equals(rateCard.getId(), vendorPartner.getVendorPartnerRateCardId()))
          .findFirst();

      if (configuredRateCard.isPresent()) {
        return configuredRateCard;
      }
    }

    if (isContractExpired(vendorPartner)) {
      return Optional.empty();
    }

    return approvedRateCards.stream().findFirst();
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
}

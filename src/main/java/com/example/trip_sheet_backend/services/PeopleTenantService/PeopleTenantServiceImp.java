package com.example.trip_sheet_backend.services.PeopleTenantService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
// import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleRequestDto;
import com.example.trip_sheet_backend.dtos.PeopleTenantCustomFieldValueDtos.PeopleTenantCustomFieldValueDto;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.PeopleTenantCustomFieldValue;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.PeopleTenant.CreatorType;
import com.example.trip_sheet_backend.models.Tenant.TenantType;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.VendorPartner;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.VendorPartnerRepository;

import jakarta.transaction.Transactional;

@Service
public class PeopleTenantServiceImp extends BaseServiceImp<PeopleTenant, UUID> implements PeopleTenantService {
  private final PeopleTenantRepository repository;
  private final TenantRepository tenantRepository;
  private final VendorPartnerRepository vendorPartnerRepository;
  private final CustomFieldRepository customFieldRepository;
  private final ModelMapper mapper;

  public PeopleTenantServiceImp(PeopleTenantRepository repository,
    TenantRepository tenantRepository, VendorPartnerRepository vendorPartnerRepository,
    CustomFieldRepository customFieldRepository, ModelMapper mapper
  ) {
    super(repository);
    this.repository = repository;
    this.tenantRepository = tenantRepository;
    this.vendorPartnerRepository = vendorPartnerRepository;
    this.customFieldRepository = customFieldRepository;
    this.mapper = mapper;
  }

  public PeopleTenant createOrGetPerson(
      CreatePeopleRequestDto dto,
      Tenant tokenTenant,
      UUID createdBy
  ) {

      final PeopleTenant additionalContact = dto.getAdditionalContactId() != null
          ? repository.findById(UUID.fromString(dto.getAdditionalContactId()))
              .orElseThrow(() -> new RuntimeException("Invalid additional contact"))
          : null;
  
      final PeopleTenant emergencyContact = dto.getEmergencyContactId() != null
          ? repository.findById(UUID.fromString(dto.getEmergencyContactId()))
              .orElseThrow(() -> new RuntimeException("Invalid emergency contact"))
          : null;

    VendorPartner ownerVendorPartner = null;
    Tenant requestedOrganisation = null;
    if (dto.getOwnerVendorPartnerId() != null) {
      if (tokenTenant.getTenantType() != TenantType.VENDOR) {
        throw new RuntimeException("Only vendors can save people under an associate customer");
      }
      ownerVendorPartner = vendorPartnerRepository.findById(UUID.fromString(dto.getOwnerVendorPartnerId()))
          .filter(link -> !Boolean.TRUE.equals(link.getIsDeleted()))
          .filter(link -> !Boolean.FALSE.equals(link.getIsActive()))
          .filter(link -> Boolean.TRUE.equals(link.getIsAssociateCustomer()))
          .filter(link -> link.getPrimaryVendor() != null
              && tokenTenant.getId().equals(link.getPrimaryVendor().getId()))
          .orElseThrow(() -> new RuntimeException("Invalid associate-customer partner link"));
      requestedOrganisation = ownerVendorPartner.getPartnerVendor();
      if (dto.getOrganisationId() != null
          && !requestedOrganisation.getId().equals(UUID.fromString(dto.getOrganisationId()))) {
        throw new RuntimeException("organisationId does not match the selected associate customer");
      }
    } else if (dto.getOrganisationId() != null) {
      requestedOrganisation = tenantRepository.findById(UUID.fromString(dto.getOrganisationId()))
          .orElseThrow(() -> new RuntimeException("Invalid organisation"));
    }

    if (tokenTenant.getTenantType() == TenantType.VENDOR
        && requestedOrganisation != null
        && requestedOrganisation.getTenantType() == TenantType.VENDOR
        && ownerVendorPartner == null) {
      throw new RuntimeException("ownerVendorPartnerId is required when saving people under an associate customer");
    }

    // Organisation creates
    if (tokenTenant.getTenantType() == TenantType.ORGANISATION) {

      return repository
          .findByPhoneAndOrganisation_Id(dto.getPhone(), tokenTenant.getId())
          .orElseGet(() -> {
              PeopleTenant person = mapper.map(dto, PeopleTenant.class);
              person.setOrganisation(tokenTenant);
              person.setCreatedBy(createdBy.toString());
              person.setCreatorType(CreatorType.ORGANISATION);
              person.setAdditionalContact(additionalContact);
              person.setEmergencyContact(emergencyContact);
              applyCustomFields(person, dto.getCustomFields());
              return repository.save(person);
          });
    }

    // CASE 3: WALK-IN (no organisationId)
    if (requestedOrganisation == null) {

      return repository
          .findByPhoneAndTenantType(dto.getPhone(), PeopleTenant.PeopleTenantType.WALKIN)
          .orElseGet(() -> {
              PeopleTenant person = mapper.map(dto, PeopleTenant.class);
              person.setTenantType(PeopleTenant.PeopleTenantType.WALKIN);
              person.setCreatedBy(createdBy.toString());
              applyCustomFields(person, dto.getCustomFields());
              return repository.save(person);
          });
    }

    // CASE 2: Vendor adding for organisation
    Tenant organisation = requestedOrganisation;

    Optional<PeopleTenant> existing = ownerVendorPartner != null
        ? repository.findByNameAndPhoneAndOrganisation_IdAndOwnerVendorPartner_Id(
            dto.getName(), dto.getPhone(), organisation.getId(), ownerVendorPartner.getId())
        : repository.findByNameAndPhoneAndOrganisation_Id(dto.getName(), dto.getPhone(), organisation.getId());

    if (existing.isPresent()) {
        PeopleTenant person = existing.get();

        // attach vendor if not already attached
        boolean alreadyAttached = person.getAttachedVendors().stream()
            .anyMatch(vendor -> vendor.getId().equals(tokenTenant.getId()));
        if (!alreadyAttached) {
            person.getAttachedVendors().add(tokenTenant);
            return repository.save(person);
        }
        return person;
    }

    // create new person for organisation
    PeopleTenant person = mapper.map(dto, PeopleTenant.class);
    person.setOrganisation(organisation);
    person.setOwnerVendorPartner(ownerVendorPartner);
    person.getAttachedVendors().add(tokenTenant);
    person.setCreatedBy(createdBy.toString());
    person.setCreatorType(CreatorType.VENDOR);
    person.setAdditionalContact(additionalContact);
    person.setEmergencyContact(emergencyContact);
    applyCustomFields(person, dto.getCustomFields());

    return repository.save(person);
  }

  private void applyCustomFields(
      PeopleTenant person,
      List<PeopleTenantCustomFieldValueDto> customFields
  ) {
    if (customFields == null) {
      return;
    }

    List<PeopleTenantCustomFieldValue> values = new ArrayList<>();

    for (PeopleTenantCustomFieldValueDto item : customFields) {
      CustomField customField = customFieldRepository.findById(UUID.fromString(item.getCustomFieldId()))
          .orElseThrow(() -> new RuntimeException("Invalid custom field"));

      PeopleTenantCustomFieldValue row = new PeopleTenantCustomFieldValue();
      row.setPeopleTenant(person);
      row.setCustomField(customField);
      row.setValue(item.getValue());

      values.add(row);
    }

    person.setCustomFieldValues(values);
  }

  @Transactional(rollbackOn = Exception.class)
  public PeopleTenant updatePhone(String Phone, UserAccount user) {
    
    if (user == null) {
      throw new RuntimeException("User not found in token");
    }

    PeopleTenant person = repository.findByEmail(user.getEmail()).orElseThrow(() -> new RuntimeException("Person not found"));

    person.setPhone(Phone);
    return repository.saveAndFlush(person);
  }

}

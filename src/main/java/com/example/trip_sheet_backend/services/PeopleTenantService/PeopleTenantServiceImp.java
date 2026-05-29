package com.example.trip_sheet_backend.services.PeopleTenantService;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
// import com.example.trip_sheet_backend.common.services.GlobalBaseServiceImp;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleRequestDto;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.PeopleTenant.CreatorType;
import com.example.trip_sheet_backend.models.Tenant.TenantType;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;

import jakarta.transaction.Transactional;

@Service
public class PeopleTenantServiceImp extends BaseServiceImp<PeopleTenant, UUID> implements PeopleTenantService {
  private final PeopleTenantRepository repository;
  private final TenantRepository tenantRepository;
  private final ModelMapper mapper;

  public PeopleTenantServiceImp(PeopleTenantRepository repository,
    TenantRepository tenantRepository, ModelMapper mapper
  ) {
    super(repository);
    this.repository = repository;
    this.tenantRepository = tenantRepository;
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
              return repository.save(person);
          });
    }

    // CASE 3: WALK-IN (no organisationId)
    if (dto.getOrganisationId() == null) {

      return repository
          .findByPhoneAndTenantType(dto.getPhone(), PeopleTenant.PeopleTenantType.WALKIN)
          .orElseGet(() -> {
              PeopleTenant person = mapper.map(dto, PeopleTenant.class);
              person.setTenantType(PeopleTenant.PeopleTenantType.WALKIN);
              person.setCreatedBy(createdBy.toString());
              return repository.save(person);
          });
    }

    // CASE 2: Vendor adding for organisation
    Tenant organisation = this.tenantRepository.findById(UUID.fromString(dto.getOrganisationId()))
        .orElseThrow(() -> new RuntimeException("Invalid organisation"));

    Optional<PeopleTenant> existing =
        repository.findByNameAndPhoneAndOrganisation_Id(dto.getName(),dto.getPhone(), organisation.getId());

    if (existing.isPresent()) {
        PeopleTenant person = existing.get();

        // attach vendor if not already attached
        if (!person.getAttachedVendors().contains(tokenTenant)) {
            person.getAttachedVendors().add(tokenTenant);
        }
        return person;
    }

    // create new person for organisation
    PeopleTenant person = mapper.map(dto, PeopleTenant.class);
    person.setOrganisation(organisation);
    person.getAttachedVendors().add(tokenTenant);
    person.setCreatedBy(createdBy.toString());
    person.setCreatorType(CreatorType.VENDOR);
    person.setAdditionalContact(additionalContact);
    person.setEmergencyContact(emergencyContact);

    return repository.save(person);
  }

  @Transactional(rollbackOn = Exception.class)
  public PeopleTenant updatePhone(UUID id, String Phone, UUID tenantId, UserAccount user) {
    PeopleTenant person = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("Person not found"));

    if (user == null) {
      throw new RuntimeException("User not found in token");
    }

    if(user.getEmail() != person.getEmail()) {
      throw new RuntimeException("Authorized user can only update their own phone number");
    }
    person.setPhone(Phone);
    return repository.save(person);
  }

}

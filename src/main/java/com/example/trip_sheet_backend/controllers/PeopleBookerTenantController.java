package com.example.trip_sheet_backend.controllers;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleBookerRequestDto;
// import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.models.PeopleBookerTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.PeopleBookerTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PeopleBookerTenantService.PeopleBookerTenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RequestMapping("/people-booker")
@RestController
public class PeopleBookerTenantController extends BaseController<PeopleBookerTenant, UUID> {

  // private final PeopleBookerTenantServiceImp peopleBookereTenantServiceImp;
  private final ModelMapper mapper;
  private final PeopleBookerTenantRepository peopleBookerTenantRepository;
  private final TenantRepository tenantRepository;


  public PeopleBookerTenantController(PeopleBookerTenantServiceImp peopleBookereTenantServiceImp, 
    PeopleBookerTenantRepository peopleBookerTenantRepository,ModelMapper mapper, TenantRepository tenantRepository) {
    super(peopleBookereTenantServiceImp);
    // this.peopleBookereTenantServiceImp = peopleBookereTenantServiceImp;
    this.mapper = mapper;
    this.peopleBookerTenantRepository = peopleBookerTenantRepository;
    this.tenantRepository = tenantRepository;
  }

@PostMapping("/create")
public ResponseEntity<ApiResponse<PeopleBookerTenant>> create(
    HttpServletRequest request,
    @Valid @RequestBody CreatePeopleBookerRequestDto body
) {

  UUID createdBy = (UUID) request.getAttribute("createdBy");
  Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

  if (tokenTenant == null) {
    throw new RuntimeException("Tenant not found in token");
  }

  // Vendor must send organisationId
  if (tokenTenant.getTenantType() == Tenant.TenantType.VENDOR &&
      body.getOrganisationId() == null) {

    throw new RuntimeException("organisationId is required for adding a booker!");
  }

  PeopleBookerTenant person = mapper.map(body, PeopleBookerTenant.class);
  person.setCreatedBy(createdBy.toString());

  // ================================
  // Resolve Organisation
  // ================================
  Tenant organisation;

  if (tokenTenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
    organisation = tokenTenant;
  } else {
    organisation = tenantRepository.findById(UUID.fromString(body.getOrganisationId()))
        .orElseThrow(() -> new RuntimeException("Invalid organisation"));
  }

  person.setOrganisation(organisation);

  // ================================
  // Set Vendor if vendor is logged in
  // ================================
  if (tokenTenant.getTenantType() == Tenant.TenantType.VENDOR) {
    person.setVendor(tokenTenant);
  }

  // ================================
  // DUPLICATE CHECK (IMPORTANT)
  // ================================
  Optional<PeopleBookerTenant> existing =
      peopleBookerTenantRepository.findByPhoneAndOrganisation_Id(
          body.getPhone(),
          organisation.getId()
      );

  if (existing.isPresent()) {
    PeopleBookerTenant existingPerson = existing.get();

    // attach vendor if missing
    if (tokenTenant.getTenantType() == Tenant.TenantType.VENDOR &&
        existingPerson.getVendor() == null) {

      existingPerson.setVendor(tokenTenant);
      peopleBookerTenantRepository.save(existingPerson);
    }

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Booker already exists", existingPerson)
    );
  }

  // ================================
  // SAVE NEW BOOKER
  // ================================
  person = peopleBookerTenantRepository.saveAndFlush(person);

  return ResponseEntity.ok(
      new ApiResponse<>(true, "Booker created successfully", person)
  );
}


}

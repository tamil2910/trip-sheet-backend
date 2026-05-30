package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
// import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleRequestDto;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.UpdatePeopleTenantRequestDto;
import com.example.trip_sheet_backend.dtos.TripChargesDtos.UpdatePhoneRequestDto;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.models.PeopleTenant.CreatorType;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RequestMapping("/people-tenant")
@RestController
public class PeopleTenantController extends BaseController<PeopleTenant, UUID> {

  private final PeopleTenantRepository peopleTenantRepository;

  private final PeopleTenantServiceImp peopleTenantServiceImp;

  public PeopleTenantController(PeopleTenantServiceImp peopleTenantServiceImp, PeopleTenantRepository peopleTenantRepository) {
    super(peopleTenantServiceImp);
    this.peopleTenantServiceImp = peopleTenantServiceImp;
    this.peopleTenantRepository = peopleTenantRepository;
  }

  @PostMapping("/create")
  public ResponseEntity<ApiResponse<PeopleTenant>> create(
      HttpServletRequest request,
      @Valid @RequestBody CreatePeopleRequestDto body
  ) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tokenTenant = (Tenant) request.getAttribute("tenant");

    if (tokenTenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    // Vendor must send organisationId OR it becomes WALKIN
    if (tokenTenant.getTenantType() == Tenant.TenantType.VENDOR &&
        body.getOrganisationId() == null &&
        body.getTenantType() != PeopleTenant.PeopleTenantType.WALKIN) {

      throw new RuntimeException(
          "organisationId is required for vendor unless person is WALKIN"
      );
    }


    PeopleTenant person = this.peopleTenantServiceImp.createOrGetPerson(
        body,
        tokenTenant,
        createdBy
    );

    return ResponseEntity.ok(
        new ApiResponse<>(
            true,
            "Person processed successfully",
            person
        )
    );
  }


  @GetMapping("/all")
  public ResponseEntity<ApiResponse<?>> getPeople(
      @RequestParam Map<String, Object> filters,
      Pageable pageable,
      HttpServletRequest request
  ) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Page<PeopleTenant> peoplePage;

    // ORGANISATION VIEW
    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {

      peoplePage = peopleTenantRepository
          .findByOrganisation_Id(tenant.getId(), pageable);
    } else {

      UUID vendorId = tenant.getId();

      boolean hasOrgFilter = filters.containsKey("organisation_id");

      // Vendor filtered by organisation
      if (hasOrgFilter) {
        UUID orgId = UUID.fromString(filters.get("organisation_id").toString());

        peoplePage = peopleTenantRepository
            .findByOrganisation_IdAndAttachedVendors_Id(orgId, vendorId, pageable);
      }

      // Vendor wants WALKIN guests
      else {
        peoplePage = peopleTenantRepository
            .findByTenantTypeAndAttachedVendors_Id(
                PeopleTenant.PeopleTenantType.WALKIN,
                vendorId,
                pageable
            );
      }
    }

    Map<String, Object> response = new HashMap<>();
    response.put("data", peoplePage.getContent());

    response.put("currentPage", peoplePage.getNumber());
    response.put("pageSize", peoplePage.getSize());
    response.put("currentPageCount", peoplePage.getNumberOfElements());
    response.put("totalItems", peoplePage.getTotalElements());
    response.put("totalPages", peoplePage.getTotalPages());

    response.put("isFirst", peoplePage.isFirst());
    response.put("isLast", peoplePage.isLast());
    response.put("hasNext", peoplePage.hasNext());
    response.put("hasPrevious", peoplePage.hasPrevious());


    return ResponseEntity.ok(
        new ApiResponse<>(true, "People list fetched successfully", response)
    );
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<ApiResponse<?>> updatePeople(
    @PathVariable UUID id,
    @Valid @RequestBody UpdatePeopleTenantRequestDto body,
    HttpServletRequest request
  ) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    PeopleTenant person = peopleTenantRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Person not found"));

    // ORGANISATION UPDATE RULE
    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {

      if (!person.getOrganisation().getId().equals(tenant.getId())) {
        throw new RuntimeException("You cannot update another organisation's people");
      }

      if (person.getCreatorType() == CreatorType.ORGANISATION) {
        applyUpdates(person, body);
      }

    }

    // VENDOR UPDATE RULE
    else {
      // Vendor can only attach themselves — not modify core data
      if (!person.getAttachedVendors().contains(tenant)) {
        person.getAttachedVendors().add(tenant);
      }
      // Vendor can only update their own data
      if (person.getCreatorType() == CreatorType.VENDOR) {
        applyUpdates(person, body);
      }
    }

    person.setUpdatedBy(createdBy.toString());

    PeopleTenant updated = peopleTenantRepository.save(person);

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Person updated successfully", updated)
    );
  }

  private void applyUpdates(PeopleTenant person, UpdatePeopleTenantRequestDto body) {
    if (body.getName() != null) person.setName(body.getName());
    if (body.getEmail() != null) person.setEmail(body.getEmail());
    if (body.getPhone() != null) person.setPhone(body.getPhone());
    if (body.getDesignation() != null) person.setDesignation(body.getDesignation());
    if (body.getGender() != null) person.setGender(body.getGender());
    if (body.getAdditionalContactId() != null) {
      person.setAdditionalContact(peopleTenantRepository.findById(UUID.fromString(body.getAdditionalContactId()))
      .orElseThrow(() -> new RuntimeException("Invalid additional contact")));
    }
    if (body.getEmergencyContactId() != null) {
      person.setEmergencyContact(peopleTenantRepository.findById(UUID.fromString(body.getEmergencyContactId()))
      .orElseThrow(() -> new RuntimeException("Invalid Emergency contact")));
    }

    if (body.getPeopleType() != null) {
      person.setPeopleType(body.getPeopleType());
    }
  }

  @Override
  @PutMapping("/{id}")
  public ApiResponse<PeopleTenant> update(
      @PathVariable UUID id,
      @Valid @RequestBody PeopleTenant payload,
      HttpServletRequest request
  ) {
    throw new RuntimeException("Direct update not supported. Please use PUT /people-tenant/update/" + id);
  }

  @PutMapping("/update-phone/{id}")
  public ResponseEntity<ApiResponse<PeopleTenant>> updatePhone(
      @PathVariable UUID id,
      @Valid @RequestBody UpdatePhoneRequestDto body,
      HttpServletRequest request
  ) {

    UUID createdBy = (UUID) request.getAttribute("createdBy");
    UserAccount user = request.getAttribute("user") != null ? (UserAccount) request.getAttribute("user") : null;

    PeopleTenant updated = peopleTenantServiceImp.updatePhone(id, body.getPhone(), user);

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Person phone updated successfully", updated)
    );
  }

}

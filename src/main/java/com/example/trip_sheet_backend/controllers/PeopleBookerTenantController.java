package com.example.trip_sheet_backend.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleBookerRequestDto;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.UpdatePeopleBookerRequestDto;
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

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<?>> getAllBookers(
      Pageable pageable,
      HttpServletRequest request
  ) {

    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    Page<PeopleBookerTenant> page;

    // Organisation sees their bookers
    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {
      page = peopleBookerTenantRepository.findByOrganisation_Id(
          tenant.getId(), pageable
      );
    }
    // Vendor sees vendor-linked bookers
    else {
      page = peopleBookerTenantRepository.findByVendor_Id(
          tenant.getId(), pageable
      );
    }

    Map<String, Object> response = new HashMap<>();
    response.put("data", page.getContent());

    response.put("currentPage", page.getNumber());
    response.put("pageSize", page.getSize());
    response.put("currentPageCount", page.getNumberOfElements());
    response.put("totalItems", page.getTotalElements());
    response.put("totalPages", page.getTotalPages());

    response.put("isFirst", page.isFirst());
    response.put("isLast", page.isLast());
    response.put("hasNext", page.hasNext());
    response.put("hasPrevious", page.hasPrevious());

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Bookers fetched successfully", response)
    );
  }

  @PutMapping("/update/{id}")
  public ResponseEntity<ApiResponse<?>> updateBooker(
      @PathVariable UUID id,
      @Valid @RequestBody UpdatePeopleBookerRequestDto body,
      HttpServletRequest request
  ) {

    UUID updatedBy = (UUID) request.getAttribute("createdBy");
    Tenant tenant = (Tenant) request.getAttribute("tenant");

    if (tenant == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    PeopleBookerTenant person = peopleBookerTenantRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Booker not found"));

    // ============================
    // ORGANISATION UPDATE RULE
    // ============================
    if (tenant.getTenantType() == Tenant.TenantType.ORGANISATION) {

      if (!person.getOrganisation().getId().equals(tenant.getId())) {
        throw new RuntimeException("You cannot update another organisation's booker");
      }

      applyBookerUpdates(person, body);
    }

    // ============================
    // VENDOR UPDATE RULE
    // ============================
    else {
      // Vendor can only attach themselves
      person.setVendor(tenant);
    }

    person.setUpdatedBy(updatedBy.toString());

    PeopleBookerTenant updated = peopleBookerTenantRepository.save(person);

    return ResponseEntity.ok(
        new ApiResponse<>(true, "Booker updated successfully", updated)
    );
  }

  private void applyBookerUpdates(PeopleBookerTenant person, UpdatePeopleBookerRequestDto body) {
    if (body.getName() != null) person.setName(body.getName());
    if (body.getEmail() != null) person.setEmail(body.getEmail());
    if (body.getPhone() != null) person.setPhone(body.getPhone());
    if (body.getDesignation() != null) person.setDesignation(body.getDesignation());
    if (body.getGender() != null) person.setGender(body.getGender());
  }

}

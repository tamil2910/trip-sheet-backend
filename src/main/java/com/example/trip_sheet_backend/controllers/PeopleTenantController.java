package com.example.trip_sheet_backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.common.controllers.BaseController;
// import com.example.trip_sheet_backend.common.controllers.GlobalBaseController;
import com.example.trip_sheet_backend.dtos.PeopleTenantDtos.CreatePeopleRequestDto;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RequestMapping("/people-tenant")
@RestController
public class PeopleTenantController extends BaseController<PeopleTenant, UUID> {

  private final PeopleTenantServiceImp peopleTenantServiceImp;



  public PeopleTenantController(PeopleTenantServiceImp peopleTenantServiceImp) {
    super(peopleTenantServiceImp);
    this.peopleTenantServiceImp = peopleTenantServiceImp;
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


}

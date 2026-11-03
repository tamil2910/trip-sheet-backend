package com.example.trip_sheet_backend.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.PeopleTenantCustomFieldValueDtos.PeopleTenantCustomFieldValueDto;
import com.example.trip_sheet_backend.models.CustomField;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.PeopleTenantCustomFieldValue;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.CustomFieldRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.PeopleTenantCustomFieldValueRepository;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.PeopleTenantService.PeopleTenantServiceImp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tenant-join-requests/my/custom-fields")
@Transactional
public class EmployeeCustomFieldController {
  private final PeopleTenantRepository peopleTenantRepository;
  private final CustomFieldRepository customFieldRepository;
  private final PeopleTenantCustomFieldValueRepository valueRepository;
  private final PeopleTenantServiceImp peopleTenantService;

  public EmployeeCustomFieldController(PeopleTenantRepository peopleTenantRepository,
      CustomFieldRepository customFieldRepository,
      PeopleTenantCustomFieldValueRepository valueRepository,
      PeopleTenantServiceImp peopleTenantService) {
    this.peopleTenantRepository = peopleTenantRepository;
    this.customFieldRepository = customFieldRepository;
    this.valueRepository = valueRepository;
    this.peopleTenantService = peopleTenantService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<CustomField>>> list(HttpServletRequest request) {
    EmployeeContext context = employeeContext(request);
    List<CustomField> fields = customFieldRepository
        .findByTenant_IdAndCreatedByVendorIsNullAndOwnerVendorPartnerIsNullAndIsDeletedFalseOrderByCreatedAtDesc(
            context.tenant().getId());
    return ResponseEntity.ok(new ApiResponse<>(true, "Employee custom fields fetched successfully", fields));
  }

  @GetMapping("/values")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyValues(HttpServletRequest request) {
    EmployeeContext context = employeeContext(request);
    List<Map<String, Object>> values = new ArrayList<>();
    if (context.person().getCustomFieldValues() != null) {
      context.person().getCustomFieldValues().stream()
          .filter(value -> value.getCustomField() != null
              && context.tenant().getId().equals(value.getCustomField().getTenant().getId())
              && value.getCreatedByVendor() == null
              && value.getCustomField().getCreatedByVendor() == null
              && value.getCustomField().getOwnerVendorPartner() == null)
          .forEach(value -> values.add(toValueMap(value)));
    }
    return ResponseEntity.ok(new ApiResponse<>(true, "Employee custom field values fetched successfully", values));
  }

  @PutMapping("/values")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> updateMyValues(
      @Valid @RequestBody List<PeopleTenantCustomFieldValueDto> body,
      HttpServletRequest request) {
    EmployeeContext context = employeeContext(request);
    List<PeopleTenantCustomFieldValueDto> submitted = body == null ? List.of() : body;
    for (PeopleTenantCustomFieldValueDto value : submitted) {
      CustomField field = customFieldRepository.findById(UUID.fromString(value.getCustomFieldId()))
          .orElseThrow(() -> new RuntimeException("Invalid custom field"));
      if (!context.tenant().getId().equals(field.getTenant().getId())
          || field.getCreatedByVendor() != null || field.getOwnerVendorPartner() != null) {
        throw new RuntimeException("Custom field does not belong to this organisation");
      }
      value.setIsVisibleToVendor(false);
    }
    peopleTenantService.replaceCustomFieldValues(context.person(), submitted, context.tenant(), context.user().getId());
    List<Map<String, Object>> values = context.person().getCustomFieldValues().stream()
        .map(this::toValueMap).toList();
    return ResponseEntity.ok(new ApiResponse<>(true, "Employee custom field values updated successfully", values));
  }

  private EmployeeContext employeeContext(HttpServletRequest request) {
    UserAccount user = (UserAccount) request.getAttribute("user");
    if (user == null || user.getTenant() == null || user.getTenantType() != UserAccount.TenantType.ORGANISATION) {
      throw new RuntimeException("You must be linked to an organisation tenant");
    }
    PeopleTenant person = peopleTenantRepository.findFirstByUserAccount_IdAndOrganisation_Id(
        user.getId(), user.getTenant().getId())
        .orElseThrow(() -> new RuntimeException("Organisation person record not found"));
    if (person.getOrganisationPersonType() != PeopleTenant.OrganisationPersonType.EMPLOYEE) {
      throw new RuntimeException("Only organisation employees can manage their custom fields");
    }
    return new EmployeeContext(user, user.getTenant(), person);
  }

  private Map<String, Object> toValueMap(PeopleTenantCustomFieldValue value) {
    Map<String, Object> row = new HashMap<>();
    row.put("id", value.getId());
    row.put("customFieldId", value.getCustomField().getId());
    row.put("customFieldName", value.getCustomField().getName());
    row.put("value", value.getValue());
    return row;
  }

  private record EmployeeContext(UserAccount user, Tenant tenant, PeopleTenant person) {}
}

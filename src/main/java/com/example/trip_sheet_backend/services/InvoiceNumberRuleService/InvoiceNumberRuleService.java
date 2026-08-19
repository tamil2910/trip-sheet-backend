package com.example.trip_sheet_backend.services.InvoiceNumberRuleService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.InvoiceNumberRuleDtos.InvoiceNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.InvoiceNumberRule;
import com.example.trip_sheet_backend.models.Tenant;

public interface InvoiceNumberRuleService {
  InvoiceNumberRule create(InvoiceNumberRuleRequestDto body, Tenant tenant, UUID createdBy);
  List<InvoiceNumberRule> getAll(Tenant tenant);
  InvoiceNumberRule getById(UUID id, Tenant tenant);
  InvoiceNumberRule update(UUID id, InvoiceNumberRuleRequestDto body, Tenant tenant, UUID updatedBy);
  void delete(UUID id, Tenant tenant, UUID deletedBy);
}

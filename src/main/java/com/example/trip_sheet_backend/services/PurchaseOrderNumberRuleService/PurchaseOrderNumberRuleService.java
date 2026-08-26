package com.example.trip_sheet_backend.services.PurchaseOrderNumberRuleService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.dtos.PurchaseOrderNumberRuleDtos.PurchaseOrderNumberRuleRequestDto;
import com.example.trip_sheet_backend.models.PurchaseOrderNumberRule;
import com.example.trip_sheet_backend.models.Tenant;

public interface PurchaseOrderNumberRuleService {
  PurchaseOrderNumberRule create(PurchaseOrderNumberRuleRequestDto body, Tenant vendor, UUID createdBy);
  List<PurchaseOrderNumberRule> getAll(Tenant vendor);
  PurchaseOrderNumberRule getById(UUID id, Tenant vendor);
  PurchaseOrderNumberRule update(UUID id, PurchaseOrderNumberRuleRequestDto body, Tenant vendor, UUID updatedBy);
  void delete(UUID id, Tenant vendor, UUID deletedBy);
}

package com.example.trip_sheet_backend.services.LabelService;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.models.Label;
import com.example.trip_sheet_backend.models.Tenant;

public interface LabelService {
  Label createLabel(String name, String color, Tenant tenant, UUID createdBy);

  Label updateLabel(UUID labelId, String name, String color, Tenant tenant, UUID updatedBy);

  List<Label> searchLabels(String searchTerm, Tenant tenant);

  void deleteLabel(UUID labelId, Tenant tenant, UUID deletedBy);
}
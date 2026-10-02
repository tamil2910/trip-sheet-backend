package com.example.trip_sheet_backend.services.LabelService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.models.Label;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.LabelRepository;

@Service
public class LabelServiceImp implements LabelService {
  private final LabelRepository labelRepository;

  public LabelServiceImp(LabelRepository labelRepository) {
    this.labelRepository = labelRepository;
  }

  @Override
  public Label createLabel(String name, String color, Tenant tenant, UUID createdBy) {
    requireTenant(tenant);
    String normalizedName = normalizeName(name);
    ensureNameAvailable(tenant.getId(), normalizedName, null);

    Label label = new Label();
    label.setName(normalizedName);
    label.setColor(normalizeColor(color));
    label.setTenant(tenant);
    if (createdBy != null) {
      label.setCreatedBy(createdBy.toString());
    }
    return labelRepository.save(label);
  }

  @Override
  public Label updateLabel(UUID labelId, String name, String color, Tenant tenant, UUID updatedBy) {
    requireTenant(tenant);
    Label label = findActiveLabel(labelId, tenant);
    String normalizedName = normalizeName(name);
    ensureNameAvailable(tenant.getId(), normalizedName, labelId);

    label.setName(normalizedName);
    label.setColor(normalizeColor(color));
    if (updatedBy != null) {
      label.setUpdatedBy(updatedBy.toString());
    }
    return labelRepository.save(label);
  }

  @Override
  public List<Label> searchLabels(String searchTerm, Tenant tenant) {
    requireTenant(tenant);
    if (searchTerm == null || searchTerm.isBlank()) {
      return labelRepository.findByTenant_IdAndIsDeletedFalseOrderByNameAsc(tenant.getId());
    }
    return labelRepository.findByTenant_IdAndIsDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(
        tenant.getId(), searchTerm.trim());
  }

  @Override
  public void deleteLabel(UUID labelId, Tenant tenant, UUID deletedBy) {
    Label label = findActiveLabel(labelId, tenant);
    label.setIsDeleted(true);
    label.setDeletedAt(Instant.now().toEpochMilli());
    if (deletedBy != null) {
      label.setDeletedBy(deletedBy.toString());
    }
    labelRepository.save(label);
  }

  private Label findActiveLabel(UUID labelId, Tenant tenant) {
    requireTenant(tenant);
    return labelRepository.findByIdAndTenant_IdAndIsDeletedFalse(labelId, tenant.getId())
        .orElseThrow(() -> new RuntimeException("Label not found"));
  }

  private void ensureNameAvailable(UUID tenantId, String name, UUID excludedLabelId) {
    boolean duplicate = excludedLabelId == null
        ? labelRepository.existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(tenantId, name)
        : labelRepository.existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalseAndIdNot(
            tenantId, name, excludedLabelId);
    if (duplicate) {
      throw new RuntimeException("Label name already exists for this tenant");
    }
  }

  private void requireTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) {
      throw new RuntimeException("Tenant not found in token");
    }
  }

  private String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new RuntimeException("Label name is required");
    }
    return name.trim();
  }

  private String normalizeColor(String color) {
    return color == null || color.isBlank() ? null : color.trim();
  }
}
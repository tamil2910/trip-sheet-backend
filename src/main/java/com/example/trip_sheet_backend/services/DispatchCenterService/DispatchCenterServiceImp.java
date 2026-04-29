package com.example.trip_sheet_backend.services.DispatchCenterService;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.common.services.BaseServiceImp;
import com.example.trip_sheet_backend.models.DispatchCenter;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.DispatchCenterRepository;

@Service
public class DispatchCenterServiceImp extends BaseServiceImp<DispatchCenter, UUID> implements DispatchCenterService {

  private final DispatchCenterRepository repository;

  public DispatchCenterServiceImp(DispatchCenterRepository repository) {
    super(repository);
    this.repository = repository;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public DispatchCenter createResource(UUID tenantId, DispatchCenter payload) {
    if (tenantId == null) {
      throw new RuntimeException("Tenant not found in token");
    }

    payload.setTenant(getTenantReference(tenantId));
    return repository.save(payload);
  }

  @Override
  @Transactional(readOnly = true)
  public DispatchCenter findByIdResource(UUID tenantId, UUID id) {
    return super.findByIdResource(tenantId, id);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DispatchCenter> getAllResources(UUID tenantId, Pageable pageable) {
    return super.getAllResources(tenantId, pageable);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public DispatchCenter updateResource(UUID tenantId, UUID id, DispatchCenter payload) {
    DispatchCenter existing = findByIdResource(tenantId, id);
    if (existing == null) {
      return null;
    }

    existing.setName(payload.getName());
    existing.setAddress(payload.getAddress());
    existing.setPhone(payload.getPhone());
    existing.setType(payload.getType());
    existing.setLatitude(payload.getLatitude());
    existing.setLongitude(payload.getLongitude());
    return repository.save(existing);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteResource(UUID tenantId, UUID id) {
    DispatchCenter existing = findByIdResource(tenantId, id);
    if (existing == null) {
      throw new RuntimeException("Dispatch center not found");
    }

    repository.delete(existing);
  }

  private Tenant getTenantReference(UUID tenantId) {
    Tenant tenant = new Tenant();
    tenant.setId(tenantId);
    return tenant;
  }
}
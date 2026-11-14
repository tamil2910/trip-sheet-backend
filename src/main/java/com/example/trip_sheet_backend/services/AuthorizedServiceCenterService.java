package com.example.trip_sheet_backend.services;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.example.trip_sheet_backend.exceptions.ResourceNotFoundException;
import com.example.trip_sheet_backend.models.AuthorizedServiceCenter;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.repositories.AuthorizedServiceCenterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class AuthorizedServiceCenterService {
  private final AuthorizedServiceCenterRepository repository;
  public AuthorizedServiceCenterService(AuthorizedServiceCenterRepository repository) { this.repository = repository; }
  public List<AuthorizedServiceCenter> list(UUID tenantId) { return repository.findByTenant_IdAndIsDeletedFalseOrderByNameAsc(tenantId); }
  public AuthorizedServiceCenter get(UUID tenantId, UUID id) { return repository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenantId).orElseThrow(() -> new ResourceNotFoundException("Authorized service center not found")); }
  public AuthorizedServiceCenter create(UUID tenantId, String name, String contactPersonName, String phoneNumber) {
    name = name.trim();
    if (repository.existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(tenantId, name)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Service center already exists");
    AuthorizedServiceCenter center = new AuthorizedServiceCenter();
    center.setName(name);
    center.setContactPersonName(contactPersonName.trim());
    center.setPhoneNumber(phoneNumber.trim());
    center.setTenant(tenant(tenantId));
    return repository.save(center);
  }
  public AuthorizedServiceCenter update(UUID tenantId, UUID id, String name, String contactPersonName, String phoneNumber) {
    AuthorizedServiceCenter center = get(tenantId, id); name = name.trim();
    if (repository.existsByTenant_IdAndNameIgnoreCaseAndIdNotAndIsDeletedFalse(tenantId, name, id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Service center already exists");
    center.setName(name);
    center.setContactPersonName(contactPersonName.trim());
    center.setPhoneNumber(phoneNumber.trim());
    return repository.save(center);
  }
  public void delete(UUID tenantId, UUID id) { AuthorizedServiceCenter center = get(tenantId, id); center.setIsDeleted(true); repository.save(center); }
  private static Tenant tenant(UUID id) { Tenant t = new Tenant(); t.setId(id); return t; }
}

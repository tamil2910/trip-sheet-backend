package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.models.TenantScoped;

public interface BaseService<T extends TenantScoped, ID extends Serializable> {
  // T createResource(T payload);

  // T findByIdResource(UUID tenatId, ID id);

  // Page<T> getAllResources(UUID tenantId ,Pageable pageable);

  // T updateResource(ID id, T payload);

  // void deleteResource(ID id);

  // Page<T> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable);

    T createResource(UUID tenantId, T payload);
    T findByIdResource(UUID tenantId, ID id);
    Page<T> getAllResources(UUID tenantId, Pageable pageable);
    T updateResource(UUID tenantId, ID id, T payload);
    void deleteResource(UUID tenantId, ID id);
    Page<T> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable);

}

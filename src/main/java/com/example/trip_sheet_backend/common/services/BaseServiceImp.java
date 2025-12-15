package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TenantScoped;

import jakarta.persistence.criteria.Predicate;

public class BaseServiceImp<T extends TenantScoped, ID extends Serializable> implements BaseService<T, ID> {

  protected final BaseRepository<T, ID> repository;
  // protected final JpaSpecificationExecutor<T> specExecutor;


  public BaseServiceImp(BaseRepository<T, ID> repository) {
    this.repository = repository;
    // this.specExecutor = specExecutor;
  }

  @Override
  public T createResource(UUID tenantId, T payload) {
    return repository.save(payload);
  }

  @Override
  public T findByIdResource(UUID tenantId, ID id) {
      T entity = repository.findById(id).orElse(null);
      if (entity == null) return null;

      UUID recordTenant = getEntityTenantId(entity);

      // If entity has no tenant -> allow access (global)
      if (recordTenant == null) {
          return entity;
      }

      // Tenant mismatch => deny access
      if (!recordTenant.equals(tenantId)) {
          throw new RuntimeException("ACCESS DENIED: Cross-tenant access forbidden");
      }

      return entity;
  }


  @Override
  public Page<T> getAllResources(UUID tenantId, Pageable pageable) {
    Specification<T> tenantSpec = (root, query, cb) -> 
            cb.equal(root.join("tenant").get("id"), tenantId);
    return repository.findAll(tenantSpec, pageable);
  }

  @Override
  public T updateResource(UUID tenantId, ID id, T payload) {
    return repository.save(payload);
  }

  @Override
  public void deleteResource(UUID tenantId, ID id) {
    repository.deleteById(id);
  }

  @Override
  public Page<T> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable) {

    // tenant-only filter
    Specification<T> tenantSpec = (root, query, cb) ->
            cb.equal(root.join("tenant").get("id"), tenantId);

    // user search filters
    Specification<T> filterSpec = buildSpecification(filters);

    // combine both
    Specification<T> combinedSpec = tenantSpec.and(filterSpec);

    return repository.findAll(combinedSpec, pageable);
  }

  private Specification<T> buildSpecification(Map<String, Object> filters) {

    return (root, query,  cb) -> {

      if (filters == null || filters.isEmpty()) 
        return cb.conjunction();

      List<Predicate> predicates = new ArrayList<>();

      filters.forEach((key, value) -> {
        if (value != null) {
          
          if (value instanceof String strVal) {
            predicates.add(cb.like(cb.lower(root.get(key)), "%" + strVal.toLowerCase()));
          } else {
            predicates.add(cb.equal(root.get(key), value));
          }
        }
      });

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private UUID getEntityTenantId(T entity) {
    try {
        // Call getTenant() on entity via reflection
        Object tenantObj = entity.getClass()
                .getMethod("getTenant")
                .invoke(entity);

        if (tenantObj == null) {
            return null;
        }

        // Get tenant.id
        return (UUID) tenantObj.getClass()
                .getMethod("getId")
                .invoke(tenantObj);

    } catch (Exception ex) {
        // Entity has no tenant field -> treat as global
        return null;
    }
  }

  


}

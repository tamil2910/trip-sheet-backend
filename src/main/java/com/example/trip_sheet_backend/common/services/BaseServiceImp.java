package com.example.trip_sheet_backend.common.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.criteria.Path; // CORRECT

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TenantScoped;
import com.example.trip_sheet_backend.models.UserAccount;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class BaseServiceImp<T extends TenantScoped, ID extends Serializable> implements BaseService<T, ID> {

  protected final BaseRepository<T, ID> repository;
  // protected final JpaSpecificationExecutor<T> specExecutor;


  public BaseServiceImp(BaseRepository<T, ID> repository) {
    this.repository = repository;
    // this.specExecutor = specExecutor;
  }

    @Override
    public T createResource(UUID tenantId, T payload) {

        UserAccount user =
            (UserAccount) RequestContextHolder
                .currentRequestAttributes()
                .getAttribute("user", RequestAttributes.SCOPE_REQUEST);

        if (user == null) {
            throw new RuntimeException("Authenticated user context missing");
        }

        // GLOBAL
        if (tenantId == null) {
            if (!"SUPER_ADMIN".equals(user.getRole().getName())) {
                throw new RuntimeException("ACCESS DENIED: Global resource creation forbidden");
            }
            payload.setTenant(null);
            return repository.save(payload);
        }

        // TENANT
        payload.setTenant(getTenantReference(tenantId));
        return repository.save(payload);
    }


    private Tenant getTenantReference(UUID tenantId) {
        Tenant t = new Tenant();
        t.setId(tenantId);
        return t;
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

//   @Override
//   public Page<T> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable) {

//     // tenant-only filter
//     Specification<T> tenantSpec = (root, query, cb) ->
//             cb.equal(root.join("tenant").get("id"), tenantId);

//     // user search filters
//     Specification<T> filterSpec = buildSpecification(filters);

//     // combine both
//     Specification<T> combinedSpec = tenantSpec.and(filterSpec);

//     return repository.findAll(combinedSpec, pageable);
//   }

//   private Specification<T> buildSpecification(Map<String, Object> filters) {

//     return (root, query, cb) -> {

//         query.distinct(true); // ⭐ IMPORTANT

//         if (filters == null || filters.isEmpty()) {
//             return cb.conjunction();
//         }

//         List<Predicate> predicates = new ArrayList<>();

//         filters.forEach((key, value) -> {

//             if (value == null) return;

//             try {
//                 Class<?> fieldType = root.get(key).getJavaType();
//                 String strVal = value.toString();

//                 // STRING fields
//                 if (fieldType.equals(String.class)) {

//                     // exact match fields
//                     if (key.equalsIgnoreCase("phone") ||
//                         key.equalsIgnoreCase("email")) {

//                         predicates.add(cb.equal(root.get(key), strVal));
//                     }
//                     // text search fields
//                     else {
//                         predicates.add(
//                             cb.like(
//                                 cb.lower(root.get(key)),
//                                 "%" + strVal.toLowerCase() + "%"
//                             )
//                         );
//                     }
//                 }
//                 // BOOLEAN / NUMBER / UUID / ENUM
//                 else {
//                     predicates.add(cb.equal(root.get(key), value));
//                 }

//             } catch (Exception ignored) {
//                 // unknown field → skip
//             }
//         });

//         return cb.and(predicates.toArray(new Predicate[0]));
//     };
// }

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

@Override
public Page<T> searchResources(UUID tenantId, Map<String, Object> filters, Pageable pageable) {
    Specification<T> spec = buildSpecification(tenantId, filters);
    return repository.findAll(spec, pageable);
}

private Specification<T> buildSpecification(UUID tenantId, Map<String, Object> filters) {

    return (root, query, cb) -> {

        // ⭐ VERY IMPORTANT: fixes duplicates + pagination
        query.distinct(true);

        List<Predicate> predicates = new ArrayList<>();

        /* ================= TENANT FILTER ================= */
        if (tenantId != null) {
            try {
                predicates.add(cb.equal(root.join("tenant").get("id"), tenantId));
            } catch (Exception ignored) {
                // entity has no tenant → ignore
            }
        }

        /* ================= DYNAMIC FILTERS ================= */
        if (filters != null) {
            filters.forEach((key, value) -> {

                if (value == null || List.of("page", "size", "sort").contains(key)) {
                    return;
                }

                // skip unknown fields early
                if (!hasField(root, key)) {
                    return;
                }

                try {
                    Path<Object> path = root.get(key);
                    Class<?> fieldType = path.getJavaType();
                    String stringValue = value.toString().trim();

                    /* ---------- STRING ---------- */
                    if (fieldType.equals(String.class)) {

                        // exact match fields
                        if (key.equalsIgnoreCase("phone") ||
                            key.equalsIgnoreCase("email")) {

                            predicates.add(cb.equal(path, stringValue));
                        }
                        // text search fields
                        else {
                            predicates.add(
                                cb.like(
                                    cb.lower(path.as(String.class)),
                                    "%" + stringValue.toLowerCase() + "%"
                                )
                            );
                        }
                    }

                    /* ---------- UUID ---------- */
                    else if (fieldType.equals(UUID.class)) {
                        UUID uuidValue = (value instanceof UUID)
                                ? (UUID) value
                                : UUID.fromString(stringValue);
                        predicates.add(cb.equal(path, uuidValue));
                    }

                    /* ---------- ENUM ---------- */
                    else if (fieldType.isEnum()) {
                        Object enumValue = Arrays.stream(fieldType.getEnumConstants())
                                .filter(e -> e.toString().equalsIgnoreCase(stringValue))
                                .findFirst()
                                .orElse(null);

                        if (enumValue != null) {
                            predicates.add(cb.equal(path, enumValue));
                        }
                    }

                    /* ---------- BOOLEAN / NUMBER ---------- */
                    else {
                        Object convertedValue = convertValue(fieldType, stringValue);
                        predicates.add(cb.equal(path, convertedValue));
                    }

                } catch (Exception ignored) {
                    // skip invalid filter safely
                }
            });
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}

  private boolean hasField(Root<T> root, String field) {
      try {
          root.get(field);
          return true;
      } catch (IllegalArgumentException e) {
          return false;
      }
  }

  private Object convertValue(Class<?> fieldType, Object value) {

      if (!(value instanceof String)) {
          return value;
      }

      String stringValue = value.toString();

      if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
          return Integer.parseInt(stringValue);
      }
      if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
          return Long.parseLong(stringValue);
      }
      if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
          return Boolean.parseBoolean(stringValue);
      }
      if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
          return Double.parseDouble(stringValue);
      }

      return value;
  }




}

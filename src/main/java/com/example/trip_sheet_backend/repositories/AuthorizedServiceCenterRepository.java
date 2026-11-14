package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.AuthorizedServiceCenter;

public interface AuthorizedServiceCenterRepository extends JpaRepository<AuthorizedServiceCenter, UUID> {
  List<AuthorizedServiceCenter> findByTenant_IdAndIsDeletedFalseOrderByNameAsc(UUID tenantId);
  Optional<AuthorizedServiceCenter> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);
  boolean existsByTenant_IdAndNameIgnoreCaseAndIsDeletedFalse(UUID tenantId, String name);
  boolean existsByTenant_IdAndNameIgnoreCaseAndIdNotAndIsDeletedFalse(UUID tenantId, String name, UUID id);
}

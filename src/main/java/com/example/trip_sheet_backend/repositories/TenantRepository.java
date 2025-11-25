package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Tenant;

@Repository
public interface TenantRepository extends BaseRepository<Tenant, UUID> {
  Optional<Tenant> findByName(String name);
}

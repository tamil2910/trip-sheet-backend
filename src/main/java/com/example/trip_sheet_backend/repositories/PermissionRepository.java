package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Permission;

public interface PermissionRepository extends BaseRepository<Permission, UUID> {
    Optional<Permission> findByName(String name);
}

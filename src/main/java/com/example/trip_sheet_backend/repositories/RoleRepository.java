package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Role;

public interface RoleRepository extends BaseRepository<Role, UUID> {
  Optional<Role> findByName(String name);
}

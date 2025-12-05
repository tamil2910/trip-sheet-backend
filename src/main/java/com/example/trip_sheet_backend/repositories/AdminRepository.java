package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

// import org.springframework.data.jpa.repository.JpaRepository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Admin;

public interface AdminRepository extends BaseRepository<Admin, UUID> {
  Optional<Admin> findByEmail(String email);
}

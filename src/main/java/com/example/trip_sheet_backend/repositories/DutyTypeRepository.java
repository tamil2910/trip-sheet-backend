package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.trip_sheet_backend.models.DutyType;

public interface DutyTypeRepository extends JpaRepository<DutyType, UUID> {
  
}

package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.Booking;

@Repository
public interface BookingRepository extends BaseRepository<Booking, UUID> {
  // List<Booking> findByTenant_Id(UUID tenantId);
  Page<Booking> findByTenant_Id(UUID tenantId, Pageable pageable);
}

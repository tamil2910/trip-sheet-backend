package com.example.trip_sheet_backend.repositories;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.DispatchCenter;

@Repository
public interface DispatchCenterRepository extends BaseRepository<DispatchCenter, UUID> {
}
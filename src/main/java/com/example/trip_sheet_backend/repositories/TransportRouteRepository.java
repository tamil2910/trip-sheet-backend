package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TransportRoute;

public interface TransportRouteRepository extends BaseRepository<TransportRoute, UUID> {
    List<TransportRoute> findByOrganisation_IdAndShift_IdAndIsDeletedFalseOrderByCreatedAtAsc(
            UUID organisationId, UUID shiftId);
}

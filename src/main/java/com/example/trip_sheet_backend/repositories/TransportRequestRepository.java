package com.example.trip_sheet_backend.repositories;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TransportRequest;

public interface TransportRequestRepository extends BaseRepository<TransportRequest, UUID> {

    List<TransportRequest> findByRoute_IdAndIsDeletedFalseOrderByCreatedAtAsc(UUID routeId);

    @Query("""
            select request from TransportRequest request
            left join fetch request.shift shift
            where request.isDeleted = false
              and ((:organisationSide = true and request.organisation.id = :tenantId)
                or (:organisationSide = false and request.tenant.id = :tenantId))
              and request.startDate <= :endDate
              and coalesce(request.endDate, request.startDate) >= :startDate
            order by shift.name asc, request.startDate asc
            """)
    List<TransportRequest> findShiftWiseInDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("organisationSide") boolean organisationSide,
            @Param("startDate") Long startDate,
            @Param("endDate") Long endDate);
}

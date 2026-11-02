package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TenantJoinRequest;
import com.example.trip_sheet_backend.models.TenantJoinRequest.RequestStatus;

public interface TenantJoinRequestRepository extends BaseRepository<TenantJoinRequest, UUID> {
    Optional<TenantJoinRequest> findByUserAccount_IdAndTenant_Id(UUID userAccountId, UUID tenantId);
    Optional<TenantJoinRequest> findByIdAndTenant_Id(UUID requestId, UUID tenantId);
    Page<TenantJoinRequest> findByTenant_IdAndStatusOrderByCreatedAtAsc(UUID tenantId, RequestStatus status, Pageable pageable);
    Page<TenantJoinRequest> findByUserAccount_IdOrderByCreatedAtDesc(UUID userAccountId, Pageable pageable);
}

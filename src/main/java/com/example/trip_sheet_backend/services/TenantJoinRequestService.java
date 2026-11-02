package com.example.trip_sheet_backend.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.CreateTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.DecideTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.TenantJoinRequestResponseDto;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.UserAccount;

public interface TenantJoinRequestService {
    TenantJoinRequestResponseDto create(UserAccount user, CreateTenantJoinRequestDto request);
    Page<TenantJoinRequestResponseDto> getMyRequests(UserAccount user, Pageable pageable);
    Page<TenantJoinRequestResponseDto> getPending(Tenant tenant, Pageable pageable);
    TenantJoinRequestResponseDto approve(Tenant tenant, UUID requestId, UserAccount admin, DecideTenantJoinRequestDto decision);
    TenantJoinRequestResponseDto reject(Tenant tenant, UUID requestId, UserAccount admin, DecideTenantJoinRequestDto decision);
    void unlinkCurrentTenant(UserAccount user);
}

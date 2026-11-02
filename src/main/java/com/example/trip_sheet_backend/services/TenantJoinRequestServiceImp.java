package com.example.trip_sheet_backend.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.CreateTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.DecideTenantJoinRequestDto;
import com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos.TenantJoinRequestResponseDto;
import com.example.trip_sheet_backend.models.PeopleTenant;
import com.example.trip_sheet_backend.models.PeopleTenant.OrganisationPersonType;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TenantJoinRequest;
import com.example.trip_sheet_backend.models.TenantJoinRequest.RequestStatus;
import com.example.trip_sheet_backend.models.UserAccount;
import com.example.trip_sheet_backend.repositories.PeopleTenantRepository;
import com.example.trip_sheet_backend.repositories.TenantJoinRequestRepository;
import com.example.trip_sheet_backend.repositories.TenantRepository;
import com.example.trip_sheet_backend.repositories.UserAccountRepository;

@Service
public class TenantJoinRequestServiceImp implements TenantJoinRequestService {
    private final TenantJoinRequestRepository requestRepository;
    private final TenantRepository tenantRepository;
    private final PeopleTenantRepository peopleTenantRepository;
    private final UserAccountRepository userAccountRepository;

    public TenantJoinRequestServiceImp(
            TenantJoinRequestRepository requestRepository,
            TenantRepository tenantRepository,
            PeopleTenantRepository peopleTenantRepository,
            UserAccountRepository userAccountRepository) {
        this.requestRepository = requestRepository;
        this.tenantRepository = tenantRepository;
        this.peopleTenantRepository = peopleTenantRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantJoinRequestResponseDto create(UserAccount user, CreateTenantJoinRequestDto body) {
        requireGuest(user);
        if (body == null || body.getTenantCode() == null || body.getTenantCode().isBlank()) {
            throw new RuntimeException("Tenant code is required");
        }
        String code = body.getTenantCode().trim().toUpperCase();
        Tenant tenant = tenantRepository.findByTenantUniqueCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Tenant not found for code: " + code));
        requireOrganisationTenant(tenant);

        if (user.getTenant() != null) {
            if (tenant.getId().equals(user.getTenant().getId())) {
                throw new RuntimeException("This user is already a member of the tenant");
            }
            throw new RuntimeException("This account is already linked to another tenant");
        }


        TenantJoinRequest request = requestRepository.findByUserAccount_IdAndTenant_Id(user.getId(), tenant.getId())
                .orElseGet(TenantJoinRequest::new);
        if (request.getId() != null && request.getStatus() == RequestStatus.APPROVED) {
            throw new RuntimeException("This join request has already been approved");
        }
        if (request.getId() != null && request.getStatus() == RequestStatus.PENDING) {
            return new TenantJoinRequestResponseDto(request);
        }

        request.setUserAccount(user);
        request.setTenant(tenant);
        request.setStatus(RequestStatus.PENDING);
        request.setApprovedPersonType(null);
        request.setDecisionNote(null);
        request.setDecidedAt(null);
        request.setDecidedByUser(null);
        request.setRequestMessage(normalize(body.getMessage()));
        request.setCreatedBy(user.getId().toString());
        request.setUpdatedBy(user.getId().toString());
        return new TenantJoinRequestResponseDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantJoinRequestResponseDto> getMyRequests(UserAccount user, Pageable pageable) {
        requireGuest(user);
        return requestRepository.findByUserAccount_IdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(TenantJoinRequestResponseDto::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantJoinRequestResponseDto> getPending(Tenant tenant, Pageable pageable) {
        requireOrganisationTenant(tenant);
        return requestRepository.findByTenant_IdAndStatusOrderByCreatedAtAsc(
                tenant.getId(), RequestStatus.PENDING, pageable)
                .map(TenantJoinRequestResponseDto::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantJoinRequestResponseDto approve(
            Tenant tenant, UUID requestId, UserAccount admin, DecideTenantJoinRequestDto decision) {
        requireOrganisationTenant(tenant);
        if (decision == null || decision.getPersonType() == null) {
            throw new RuntimeException("personType must be EMPLOYEE or VISITOR");
        }
        TenantJoinRequest request = requirePendingRequest(tenant, requestId);
        UserAccount user = request.getUserAccount();
        if (user.getTenant() != null && !tenant.getId().equals(user.getTenant().getId())) {
            throw new RuntimeException("This account is already linked to another tenant");
        }

        PeopleTenant person = peopleTenantRepository
                .findFirstByUserAccount_IdAndOrganisation_Id(user.getId(), tenant.getId())
                .orElseGet(() -> findExistingPersonByIdentity(user, tenant).orElseGet(PeopleTenant::new));
        if (person.getId() == null) {
            person.setName(user.getUsername());
            person.setEmail(user.getEmail());
            person.setPhone(user.getPhone());
            person.setOrganisation(tenant);
            person.setTenantType(PeopleTenant.PeopleTenantType.ORGANISATION);
            person.setCreatorType(PeopleTenant.CreatorType.ORGANISATION);
            person.setCreatedBy(admin.getId().toString());
        } else if (person.getUserAccount() != null && !user.getId().equals(person.getUserAccount().getId())) {
            throw new RuntimeException("A different user account is already linked to this organisation person");
        }
        person.setUserAccount(user);
        person.setOrganisationPersonType(decision.getPersonType());
        person.setUpdatedBy(admin.getId().toString());
        peopleTenantRepository.save(person);

        user.setTenant(tenant);
        user.setTenantType(UserAccount.TenantType.ORGANISATION);
        user.setUpdatedBy(admin.getId().toString());
        userAccountRepository.save(user);

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedPersonType(decision.getPersonType());
        request.setDecisionNote(normalize(decision.getDecisionNote()));
        request.setDecidedAt(Instant.now().toEpochMilli());
        request.setDecidedByUser(admin);
        request.setUpdatedBy(admin.getId().toString());
        return new TenantJoinRequestResponseDto(requestRepository.save(request));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantJoinRequestResponseDto reject(
            Tenant tenant, UUID requestId, UserAccount admin, DecideTenantJoinRequestDto decision) {
        requireOrganisationTenant(tenant);
        TenantJoinRequest request = requirePendingRequest(tenant, requestId);
        request.setStatus(RequestStatus.REJECTED);
        request.setDecisionNote(decision == null ? null : normalize(decision.getDecisionNote()));
        request.setDecidedAt(Instant.now().toEpochMilli());
        request.setDecidedByUser(admin);
        request.setUpdatedBy(admin.getId().toString());
        return new TenantJoinRequestResponseDto(requestRepository.save(request));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkCurrentTenant(UserAccount user) {
        requireGuest(user);
        Tenant tenant = user.getTenant();
        if (tenant == null || tenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("This guest account is not linked to an organisation tenant");
        }

        for (PeopleTenant person : peopleTenantRepository
                .findAllByUserAccount_IdAndOrganisation_Id(user.getId(), tenant.getId())) {
            person.setUserAccount(null);
            person.setUpdatedBy(user.getId().toString());
            peopleTenantRepository.save(person);
        }

        user.setTenant(null);
        user.setTenantType(null);
        user.setUpdatedBy(user.getId().toString());
        userAccountRepository.save(user);

        TenantJoinRequest joinRequest = requestRepository
                .findByUserAccount_IdAndTenant_Id(user.getId(), tenant.getId())
                .orElseGet(TenantJoinRequest::new);
        joinRequest.setUserAccount(user);
        joinRequest.setTenant(tenant);
        joinRequest.setStatus(RequestStatus.WITHDRAWN);
        joinRequest.setDecisionNote("Withdrawn by guest");
        joinRequest.setDecidedAt(Instant.now().toEpochMilli());
        joinRequest.setDecidedByUser(user);
        joinRequest.setCreatedBy(user.getId().toString());
        joinRequest.setUpdatedBy(user.getId().toString());
        requestRepository.save(joinRequest);
    }
    private java.util.Optional<PeopleTenant> findExistingPersonByIdentity(UserAccount user, Tenant tenant) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            java.util.Optional<PeopleTenant> byEmail = peopleTenantRepository
                    .findFirstByEmailIgnoreCaseAndOrganisation_Id(user.getEmail(), tenant.getId());
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return peopleTenantRepository.findFirstByPhoneAndOrganisation_Id(user.getPhone(), tenant.getId());
        }
        return java.util.Optional.empty();
    }

    private TenantJoinRequest requirePendingRequest(Tenant tenant, UUID requestId) {
        TenantJoinRequest request = requestRepository.findByIdAndTenant_Id(requestId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Tenant join request not found"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be decided");
        }
        return request;
    }

    private void requireGuest(UserAccount user) {
        if (user == null || user.getId() == null || user.getRole() == null
                || !"GUEST".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Only authenticated GUEST users can submit join requests");
        }
    }

    private void requireOrganisationTenant(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || tenant.getTenantType() != Tenant.TenantType.ORGANISATION) {
            throw new RuntimeException("Organisation tenant context is required");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

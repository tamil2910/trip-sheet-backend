package com.example.trip_sheet_backend.dtos.TenantJoinRequestDtos;

import java.util.UUID;

import com.example.trip_sheet_backend.models.TenantJoinRequest;
import com.example.trip_sheet_backend.models.PeopleTenant.OrganisationPersonType;
import com.example.trip_sheet_backend.models.TenantJoinRequest.RequestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantJoinRequestResponseDto {
    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private UUID userAccountId;
    private String name;
    private String email;
    private UUID approverUserAccountId;
    private String approverName;
    private String approverEmail;
    private String phone;
    private String message;
    private RequestStatus status;
    private OrganisationPersonType personType;
    private String decisionNote;
    private Long requestedAt;
    private Long decidedAt;

    public TenantJoinRequestResponseDto(TenantJoinRequest request) {
        this.id = request.getId();
        this.tenantId = request.getTenant().getId();
        this.tenantName = request.getTenant().getTenantName();
        this.userAccountId = request.getUserAccount().getId();
        this.name = request.getUserAccount().getUsername();
        this.email = request.getUserAccount().getEmail();
        this.approverUserAccountId = request.getApproverUserAccount() == null
                ? null : request.getApproverUserAccount().getId();
        this.approverName = request.getApproverName();
        this.approverEmail = request.getApproverEmail();
        this.phone = request.getUserAccount().getPhone();
        this.message = request.getRequestMessage();
        this.status = request.getStatus();
        this.personType = request.getApprovedPersonType();
        this.decisionNote = request.getDecisionNote();
        this.requestedAt = request.getCreatedAt();
        this.decidedAt = request.getDecidedAt();
    }
}

package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import com.example.trip_sheet_backend.models.PeopleTenant.OrganisationPersonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tenant_join_requests",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tenant_join_request_user_tenant",
        columnNames = {"user_account_id", "tenant_id"}
    ),
    indexes = @Index(name = "idx_tenant_join_request_tenant_status", columnList = "tenant_id, status")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantJoinRequest extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "approved_person_type", length = 20)
    private OrganisationPersonType approvedPersonType;

    @Column(name = "request_message", length = 1000)
    private String requestMessage;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "decided_at")
    private Long decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private UserAccount decidedByUser;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED, WITHDRAWN
    }
}

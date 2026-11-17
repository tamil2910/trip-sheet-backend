package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_participant_access_grants")
@Getter
@Setter
@NoArgsConstructor
public class EventParticipantAccessGrant extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participant_request_id", nullable = false)
  private EventParticipantRequest participantRequest;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Long expiresAt;

  @Column(name = "consumed_at")
  private Long consumedAt;

  @Column(name = "revoked_at")
  private Long revokedAt;
}
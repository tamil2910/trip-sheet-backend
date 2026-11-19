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
@Table(name = "transport_event_request_invites")
@Getter
@Setter
@NoArgsConstructor
public class EventRequestInvite extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private TransportEvent event;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(length = 32)
  private String phone;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "request_link", length = 2048)
  private String requestLink;

  @Column(name = "expires_at", nullable = false)
  private Long expiresAt;

  @Column(name = "revoked_at")
  private Long revokedAt;

  @Column(name = "consumed_at")
  private Long consumedAt;
}

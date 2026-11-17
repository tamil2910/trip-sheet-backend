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
@Table(name = "event_participant_otp_challenges")
@Getter
@Setter
@NoArgsConstructor
public class EventParticipantOtpChallenge extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "access_grant_id", nullable = false)
  private EventParticipantAccessGrant accessGrant;

  @Column(name = "otp_hash", nullable = false, length = 100)
  private String otpHash;

  @Column(name = "expires_at", nullable = false)
  private Long expiresAt;

  @Column(nullable = false)
  private Integer attempts = 0;

  @Column(name = "consumed_at")
  private Long consumedAt;
}
package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_event_participant_requests")
@Getter
@Setter
@NoArgsConstructor
public class EventParticipantRequest extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private TransportEvent event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invite_id")
  private EventRequestInvite invite;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "people_tenant_id")
  private PeopleTenant peopleTenant;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 320)
  private String email;

  @Column(length = 32)
  private String phone;

  @Column(name = "pickup_address", length = 1000)
  private String pickupAddress;

  @Column(name = "pickup_latitude", nullable = false)
  private Double pickupLatitude;

  @Column(name = "pickup_longitude", nullable = false)
  private Double pickupLongitude;

  @Column(length = 2000)
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private RequestStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_method", nullable = false, length = 20)
  private MatchMethod matchMethod;

  public enum RequestStatus { APPROVED, PENDING_APPROVAL, REJECTED, WITHDRAWN }
  public enum MatchMethod { EMAIL, PHONE, BOTH, NONE, AMBIGUOUS }
}
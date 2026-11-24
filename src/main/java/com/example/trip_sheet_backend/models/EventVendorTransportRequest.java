package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_vendor_transport_requests")
@Getter @Setter @NoArgsConstructor
public class EventVendorTransportRequest extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private TransportEvent event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Tenant vendor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private RequestFormat format;

  @Column(name = "transport_type", nullable = false, length = 32)
  private String transportType;

  @Column(name = "vehicle_type_id", nullable = false)
  private java.util.UUID vehicleTypeId;

  @Column(name = "duty_type_id", nullable = false)
  private java.util.UUID dutyTypeId;

  @Column(name = "pickup_time", nullable = false)
  private Long pickupTime;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "event_vendor_transport_request_participants",
      joinColumns = @JoinColumn(name = "transport_request_id"),
      inverseJoinColumns = @JoinColumn(name = "participant_request_id"))
  private List<EventParticipantRequest> participants = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RequestStatus status = RequestStatus.SENT;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_id")
  private Trip trip;

  public enum RequestFormat { INDIVIDUAL, GROUPED }
  public enum RequestStatus { SENT, ACCEPTED, DECLINED, ALLOTTED }
}

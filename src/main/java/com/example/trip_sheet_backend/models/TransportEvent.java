package com.example.trip_sheet_backend.models;

import java.util.ArrayList;
import java.util.List;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_events")
@Getter
@Setter
@NoArgsConstructor
public class TransportEvent extends BaseModel {
  @NotBlank
  @Column(nullable = false, length = 255)
  private String name;

  @NotBlank
  @Column(nullable = false, length = 500)
  private String venue;

  @Column(length = 255)
  private String city;

  @Column(length = 4000)
  private String description;

  @Column(name = "venue_latitude")
  private Double venueLatitude;

  @Column(name = "venue_longitude")
  private Double venueLongitude;

  @Column(name = "starts_at", nullable = false)
  private Long startsAt;

  @Column(name = "ends_at", nullable = false)
  private Long endsAt;

  @Column(name = "link_valid_until", nullable = false)
  private Long linkValidUntil;

  @Column(name = "public_request_token_hash", unique = true, length = 64)
  private String publicRequestTokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_audience", nullable = false, length = 32)
  private RequestAudience requestAudience = RequestAudience.ANYONE_WITH_LINK;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventStatus status = EventStatus.OPEN;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "transport_event_vehicle_types",
      joinColumns = @JoinColumn(name = "event_id"),
      inverseJoinColumns = @JoinColumn(name = "vehicle_type_id"))
  private List<VehicleType> vehicleTypes = new ArrayList<>();

  public enum RequestAudience { ANYONE_WITH_LINK, SPECIFIC_PEOPLE }
  public enum EventStatus { OPEN, CLOSED, CANCELLED }
}






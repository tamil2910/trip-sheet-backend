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
@Table(name = "transport_event_participant_extra_passengers")
@Getter
@Setter
@NoArgsConstructor
public class EventParticipantFamilyMember extends BaseModel {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participant_request_id", nullable = false)
  private EventParticipantRequest participantRequest;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 32)
  private String gender;

  @Column(name = "relationship_type", nullable = false, length = 64)
  private String relationshipType;
}

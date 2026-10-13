package com.example.trip_sheet_backend.models;

import com.example.trip_sheet_backend.common.models.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_booking_drafts")
@Getter @Setter @NoArgsConstructor
public class ChatBookingDraft extends BaseModel {
  @ManyToOne(optional = false) @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;
  @Column(nullable = false, columnDefinition = "TEXT") private String requestText;
  @Column(nullable = false, columnDefinition = "TEXT") private String bookingJson;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;
  private Long expiresAt;
  private String tripId;
  public enum Status { PENDING, CONFIRMED, EXPIRED }
}

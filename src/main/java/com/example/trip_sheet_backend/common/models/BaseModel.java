package com.example.trip_sheet_backend.common.models;

import java.time.Instant;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class BaseModel {
  @Id
  @GeneratedValue
  private UUID id;
  private Long createdAt;
  private Long updatedAt;
  private Long deletedAt;

  private String createdBy;
  private String updatedBy;
  private String deletedBy;
  private Boolean isDeleted;

  @PrePersist
  public void preCreate() {
    long now = Instant.now().toEpochMilli();
    this.createdAt = now;
    this.updatedAt = now;
    this.isDeleted = false;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now().toEpochMilli();
  }
}

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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class BaseModel {
  @Id
  @GeneratedValue
  private UUID id;

  @JsonIgnore
  private Long createdAt;

  @JsonIgnore
  private Long updatedAt;

  @JsonIgnore
  private Long deletedAt;

  @JsonIgnore
  private String createdBy;

  @JsonIgnore
  private String updatedBy;

  @JsonIgnore
  private String deletedBy;

  @JsonIgnore
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

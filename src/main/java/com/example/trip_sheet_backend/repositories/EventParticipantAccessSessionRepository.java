package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.EventParticipantAccessSession;

public interface EventParticipantAccessSessionRepository extends BaseRepository<EventParticipantAccessSession, UUID> {
  Optional<EventParticipantAccessSession> findByTokenHashAndIsDeletedFalse(String tokenHash);
  java.util.List<EventParticipantAccessSession> findAllByAccessGrant_ParticipantRequest_IdAndRevokedAtIsNullAndIsDeletedFalse(java.util.UUID requestId);
}
package com.example.trip_sheet_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.TransportRouteDraft;

public interface TransportRouteDraftRepository extends BaseRepository<TransportRouteDraft, UUID> {
    List<TransportRouteDraft> findByTenant_IdAndIsDeletedFalseOrderByCreatedAtDesc(UUID tenantId);

    Optional<TransportRouteDraft> findByIdAndTenant_IdAndIsDeletedFalse(UUID id, UUID tenantId);

    List<TransportRouteDraft> findByTenant_IdAndDraftStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID tenantId, TransportRouteDraft.DraftStatus draftStatus);

    @Modifying
    @Query("update TransportRouteDraft draft set draft.draftStatus = :expired "
            + "where draft.draftStatus = :active and draft.expiresAt <= :now and draft.isDeleted = false")
    int expireActiveDrafts(@Param("active") TransportRouteDraft.DraftStatus active,
            @Param("expired") TransportRouteDraft.DraftStatus expired,
            @Param("now") long now);
}

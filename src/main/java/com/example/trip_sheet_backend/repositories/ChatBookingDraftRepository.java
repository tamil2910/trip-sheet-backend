package com.example.trip_sheet_backend.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.example.trip_sheet_backend.common.repositories.BaseRepository;
import com.example.trip_sheet_backend.models.ChatBookingDraft;

public interface ChatBookingDraftRepository extends BaseRepository<ChatBookingDraft, UUID> {
  Optional<ChatBookingDraft> findByIdAndTenant_Id(UUID id, UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select draft from ChatBookingDraft draft where draft.id = :id and draft.tenant.id = :tenantId")
  Optional<ChatBookingDraft> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}

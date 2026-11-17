package com.example.trip_sheet_backend.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.trip_sheet_backend.dtos.EventDtos.EventCreateRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventMutationResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventMutationResponse.RecipientRequestLink;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRecipientRequest;
import com.example.trip_sheet_backend.dtos.EventDtos.EventResponse;
import com.example.trip_sheet_backend.models.EventRequestInvite;
import com.example.trip_sheet_backend.models.Tenant;
import com.example.trip_sheet_backend.models.TransportEvent;
import com.example.trip_sheet_backend.models.VehicleType;
import com.example.trip_sheet_backend.repositories.EventRequestInviteRepository;
import com.example.trip_sheet_backend.repositories.TransportEventRepository;
import com.example.trip_sheet_backend.repositories.VehicleTypeRepository;

@Service
public class TransportEventService {
  private static final Logger log = LoggerFactory.getLogger(TransportEventService.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final TransportEventRepository eventRepository;
  private final EventRequestInviteRepository inviteRepository;
  private final VehicleTypeRepository vehicleTypeRepository;
  private final EmailService emailService;
  private final String requestLinkBaseUrl;

  public TransportEventService(
      TransportEventRepository eventRepository,
      EventRequestInviteRepository inviteRepository,
      VehicleTypeRepository vehicleTypeRepository,
      EmailService emailService,
      @Value("${app.event.request-link-base:http://localhost:4200/event-request}") String requestLinkBaseUrl) {
    this.eventRepository = eventRepository;
    this.inviteRepository = inviteRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.emailService = emailService;
    this.requestLinkBaseUrl = requestLinkBaseUrl;
  }

  @Transactional
  public EventMutationResponse create(EventCreateRequest request, Tenant tenant, UUID createdBy) {
    requireTenant(tenant);
    validateEvent(request);
    validateRecipientsForAudience(request);

    TransportEvent event = new TransportEvent();
    applyEventFields(event, request);
    event.setTenant(tenant);
    event.setVehicleTypes(resolveVehicleTypes(request.getVehicleTypeIds()));
    event.setCreatedBy(createdBy == null ? null : createdBy.toString());
    event.setUpdatedBy(createdBy == null ? null : createdBy.toString());

    String requestLink = null;
    if (request.getRequestAudience() == TransportEvent.RequestAudience.ANYONE_WITH_LINK) {
      requestLink = issuePublicRequestLink(event);
    }
    event = eventRepository.saveAndFlush(event);

    List<RecipientRequestLink> recipientLinks = List.of();
    if (request.getRequestAudience() == TransportEvent.RequestAudience.SPECIFIC_PEOPLE
        && request.getRecipients() != null && !request.getRecipients().isEmpty()) {
      recipientLinks = replaceRecipientInvites(event, request.getRecipients(), createdBy);
      scheduleInvitationEmails(event.getName(), event.getLinkValidUntil(), recipientLinks);
    }
    return new EventMutationResponse(EventResponse.from(event), requestLink, recipientLinks);
  }

  @Transactional
  public EventMutationResponse update(UUID id, EventCreateRequest request, Tenant tenant, UUID updatedBy) {
    requireTenant(tenant);
    validateEvent(request);
    validateRecipientsForAudience(request);

    TransportEvent event = eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    TransportEvent.RequestAudience previousAudience = event.getRequestAudience();
    boolean audienceChanged = previousAudience != request.getRequestAudience();
    boolean expiryChanged = !event.getLinkValidUntil().equals(request.getLinkValidUntil());

    applyEventFields(event, request);
    event.setVehicleTypes(resolveVehicleTypes(request.getVehicleTypeIds()));
    event.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());

    String requestLink = null;
    List<RecipientRequestLink> recipientLinks = List.of();
    if (request.getRequestAudience() == TransportEvent.RequestAudience.ANYONE_WITH_LINK) {
      if (audienceChanged) revokeActiveInvites(event);
      if (audienceChanged || event.getPublicRequestTokenHash() == null) {
        requestLink = issuePublicRequestLink(event);
      }
    } else {
      event.setPublicRequestTokenHash(null);
      if (audienceChanged) revokeActiveInvites(event);
      if (request.getRecipients() != null) {
        recipientLinks = replaceRecipientInvites(event, request.getRecipients(), updatedBy);
        scheduleInvitationEmails(event.getName(), event.getLinkValidUntil(), recipientLinks);
      } else if (expiryChanged) {
        updateActiveInviteExpiry(event, request.getLinkValidUntil());
      }
    }

    event = eventRepository.saveAndFlush(event);
    return new EventMutationResponse(EventResponse.from(event), requestLink, recipientLinks);
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> list(Tenant tenant, Pageable pageable) {
    requireTenant(tenant);
    List<TransportEvent> all = eventRepository.findAllByTenant_IdAndIsDeletedFalseOrderByStartsAtAsc(tenant.getId());
    int page = pageable == null ? 0 : pageable.getPageNumber();
    int size = pageable == null || pageable.isUnpaged() ? Math.max(all.size(), 1) : pageable.getPageSize();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    return new PageImpl<>(all.subList(from, to).stream().map(EventResponse::from).toList(),
        pageable == null ? Pageable.unpaged() : pageable, all.size());
  }

  @Transactional
  public EventResponse updateVehicleTypes(UUID id, List<UUID> vehicleTypeIds, Tenant tenant, UUID updatedBy) {
    requireTenant(tenant);
    TransportEvent event = eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    event.setVehicleTypes(resolveVehicleTypes(vehicleTypeIds));
    event.setUpdatedBy(updatedBy == null ? null : updatedBy.toString());
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional(readOnly = true)
  public EventResponse get(UUID id, Tenant tenant) {
    requireTenant(tenant);
    return eventRepository.findByIdAndTenant_IdAndIsDeletedFalse(id, tenant.getId())
        .map(EventResponse::from)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));
  }

  @Transactional(readOnly = true)
  public List<EventResponse.VehicleTypeOption> vehicleTypeOptions() {
    return vehicleTypeRepository.findAll().stream()
        .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
        .map(EventResponse.VehicleTypeOption::from)
        .toList();
  }

  private void applyEventFields(TransportEvent event, EventCreateRequest request) {
    event.setName(request.getName().trim());
    event.setVenue(request.getVenue().trim());
    event.setCity(trimToNull(request.getCity()));
    event.setDescription(trimToNull(request.getDescription()));
    validateVenueCoordinates(request.getVenueLatitude(), request.getVenueLongitude());
    event.setVenueLatitude(request.getVenueLatitude());
    event.setVenueLongitude(request.getVenueLongitude());
    event.setStartsAt(request.getStartsAt());
    event.setEndsAt(request.getEndsAt());
    event.setLinkValidUntil(request.getLinkValidUntil());
    event.setRequestAudience(request.getRequestAudience());
  }

  private void validateEvent(EventCreateRequest request) {
    if (request.getStartsAt() == null || request.getEndsAt() == null || request.getLinkValidUntil() == null) {
      throw new IllegalArgumentException("Event start, end, and link validity are required");
    }
    long now = Instant.now().toEpochMilli();
    if (request.getStartsAt() >= request.getEndsAt()) throw new IllegalArgumentException("Event end must be after its start");
    if (request.getLinkValidUntil() <= now) throw new IllegalArgumentException("Link validity must be in the future");
    if (request.getLinkValidUntil() < request.getStartsAt()) throw new IllegalArgumentException("Link must remain valid through the event start");
    if (request.getRequestAudience() == null) throw new IllegalArgumentException("Request audience is required");
  }

  private void validateRecipientsForAudience(EventCreateRequest request) {
    List<EventRecipientRequest> recipients = request.getRecipients();
    if (request.getRequestAudience() == TransportEvent.RequestAudience.ANYONE_WITH_LINK
        && recipients != null && !recipients.isEmpty()) {
      throw new IllegalArgumentException("Recipients can only be provided for SPECIFIC_PEOPLE events");
    }
    if (recipients == null || recipients.isEmpty()) return;
    Set<String> emails = new HashSet<>();
    for (EventRecipientRequest recipient : recipients) {
      if (recipient == null || recipient.getName() == null || recipient.getName().isBlank()
          || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
        throw new IllegalArgumentException("Each event recipient requires a name and email");
      }
      String normalizedEmail = recipient.getEmail().trim().toLowerCase(Locale.ROOT);
      if (!emails.add(normalizedEmail)) throw new IllegalArgumentException("Duplicate recipient emails are not allowed");
    }
  }

  private String issuePublicRequestLink(TransportEvent event) {
    String token = newToken();
    event.setPublicRequestTokenHash(hashToken(token));
    return requestLinkBaseUrl.replaceAll("/+$", "") + "/" + token;
  }

  private List<RecipientRequestLink> replaceRecipientInvites(
      TransportEvent event, List<EventRecipientRequest> recipients, UUID actorId) {
    revokeActiveInvites(event);
    if (recipients.isEmpty()) return List.of();

    List<EventRequestInvite> invites = new ArrayList<>();
    List<RecipientRequestLink> links = new ArrayList<>();
    for (EventRecipientRequest recipient : recipients) {
      String token = newToken();
      String email = recipient.getEmail().trim().toLowerCase(Locale.ROOT);
      EventRequestInvite invite = new EventRequestInvite();
      invite.setEvent(event);
      invite.setName(recipient.getName().trim());
      invite.setEmail(email);
      invite.setPhone(trimToNull(recipient.getPhone()));
      invite.setTokenHash(hashToken(token));
      invite.setExpiresAt(event.getLinkValidUntil());
      invite.setCreatedBy(actorId == null ? null : actorId.toString());
      invite.setUpdatedBy(actorId == null ? null : actorId.toString());
      invites.add(invite);
      links.add(new RecipientRequestLink(invite.getName(), email,
          requestLinkBaseUrl.replaceAll("/+$", "") + "/" + token));
    }
    inviteRepository.saveAll(invites);
    return List.copyOf(links);
  }

  private void revokeActiveInvites(TransportEvent event) {
    long now = Instant.now().toEpochMilli();
    List<EventRequestInvite> active = inviteRepository
        .findAllByEvent_IdAndRevokedAtIsNullAndIsDeletedFalse(event.getId());
    active.forEach(invite -> invite.setRevokedAt(now));
    inviteRepository.saveAll(active);
  }

  private void updateActiveInviteExpiry(TransportEvent event, Long expiresAt) {
    List<EventRequestInvite> active = inviteRepository
        .findAllByEvent_IdAndRevokedAtIsNullAndIsDeletedFalse(event.getId());
    active.forEach(invite -> invite.setExpiresAt(expiresAt));
    inviteRepository.saveAll(active);
  }

  private void scheduleInvitationEmails(String eventName, Long expiresAt, List<RecipientRequestLink> links) {
    if (links.isEmpty()) return;
    Runnable sendEmails = () -> links.forEach(link -> {
      try {
        emailService.sendEventRequestLink(link.email(), link.name(), eventName, link.requestLink(), expiresAt);
      } catch (RuntimeException ex) {
        log.error("Could not send event request link for event '{}' to {}", eventName, link.email(), ex);
      }
    });
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { sendEmails.run(); }
      });
    } else {
      sendEmails.run();
    }
  }

  private List<VehicleType> resolveVehicleTypes(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    if (new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Duplicate vehicle type ids are not allowed");
    List<VehicleType> result = vehicleTypeRepository.findAllById(ids);
    Set<UUID> found = new HashSet<>();
    result.forEach(type -> found.add(type.getId()));
    if (found.size() != ids.size()) throw new IllegalArgumentException("One or more vehicle types were not found");
    if (result.stream().anyMatch(type -> Boolean.TRUE.equals(type.getIsDeleted()))) {
      throw new IllegalArgumentException("Deleted vehicle types cannot be selected");
    }
    return result;
  }

  private void validateVenueCoordinates(Double latitude, Double longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw new IllegalArgumentException("Venue latitude and longitude must be provided together");
    }
    if (latitude != null && (latitude < -90 || latitude > 90)) {
      throw new IllegalArgumentException("Venue latitude must be between -90 and 90");
    }
    if (longitude != null && (longitude < -180 || longitude > 180)) {
      throw new IllegalArgumentException("Venue longitude must be between -180 and 180");
    }
  }

  private void requireTenant(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) throw new IllegalArgumentException("Tenant not found in token");
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

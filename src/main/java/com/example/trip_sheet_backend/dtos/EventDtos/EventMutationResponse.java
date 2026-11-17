package com.example.trip_sheet_backend.dtos.EventDtos;

import java.util.List;

public record EventMutationResponse(
    EventResponse event,
    String requestLink,
    List<RecipientRequestLink> recipientLinks) {

  public record RecipientRequestLink(String name, String email, String requestLink) { }
}

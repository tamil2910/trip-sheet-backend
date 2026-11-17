package com.example.trip_sheet_backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestLinkResponse;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestSubmission;
import com.example.trip_sheet_backend.dtos.EventDtos.EventRequestSubmissionResponse;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.EventParticipantRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/event-requests")
public class PublicEventRequestController {
  private final EventParticipantRequestService requestService;

  public PublicEventRequestController(EventParticipantRequestService requestService) {
    this.requestService = requestService;
  }

  @GetMapping("/{token}")
  public ApiResponse<EventRequestLinkResponse> inspect(@PathVariable String token) {
    return new ApiResponse<>(true, "Event request details fetched", requestService.inspectLink(token));
  }

  @PostMapping("/{token}")
  public ApiResponse<EventRequestSubmissionResponse> submit(
      @PathVariable String token, @Valid @RequestBody EventRequestSubmission body) {
    return new ApiResponse<>(true, "Event request submitted", requestService.submit(token, body));
  }
}
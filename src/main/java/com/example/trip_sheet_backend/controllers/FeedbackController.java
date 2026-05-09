package com.example.trip_sheet_backend.controllers;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.trip_sheet_backend.dtos.TripDtos.TripFeedbackRequestDTO;
import com.example.trip_sheet_backend.dtos.TripDtos.TripFeedbackResponseDTO;
import com.example.trip_sheet_backend.models.TripFeedback;
import com.example.trip_sheet_backend.response_setups.ApiResponse;
import com.example.trip_sheet_backend.services.TripFeedbackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/feedback")
@Validated
public class FeedbackController {

    private final TripFeedbackService tripFeedbackService;

    public FeedbackController(TripFeedbackService tripFeedbackService) {
        this.tripFeedbackService = tripFeedbackService;
    }

    @PreAuthorize("permitAll()")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<?>> submitFeedback(
            @RequestParam("token") String token,
            @Valid @RequestBody TripFeedbackRequestDTO request) {
        try {
            Optional<TripFeedback> feedback = tripFeedbackService.submitFeedback(token, request);
            if (feedback.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "We received your feedback, thanks", null));
            }

            TripFeedbackResponseDTO response = TripFeedbackResponseDTO.fromEntity(feedback.get());
            return ResponseEntity.ok(new ApiResponse<>(true, "Feedback submitted successfully", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, ex.getMessage(), null));
        }
    }
}
package com.example.trip_sheet_backend.services.TripService;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.trip_sheet_backend.services.TripBillingService.TripBillingService;
import com.example.trip_sheet_backend.services.TripFeedbackService;

@Service
public class TripCompletionWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(TripCompletionWorkflowService.class);

  private final TripBillingService tripBillingService;
  private final TripFeedbackService tripFeedbackService;
  private final TripRealtimePublisher tripRealtimePublisher;

  public TripCompletionWorkflowService(
      TripBillingService tripBillingService,
      TripFeedbackService tripFeedbackService,
      TripRealtimePublisher tripRealtimePublisher
  ) {
    this.tripBillingService = tripBillingService;
    this.tripFeedbackService = tripFeedbackService;
    this.tripRealtimePublisher = tripRealtimePublisher;
  }

  @Async
  public void runAfterTripCompletion(UUID tripId) {
    if (tripId == null) {
      return;
    }

    try {
      tripBillingService.generatePurchaseOrdersForTrip(tripId);
    } catch (Exception ex) {
      log.error("Failed to generate purchase orders for completed trip {}", tripId, ex);
    }

    try {
      tripBillingService.generatePurchaseInvoicesForTrip(tripId);
    } catch (Exception ex) {
      log.error("Failed to generate purchase invoices for completed trip {}", tripId, ex);
    }

    try {
      tripFeedbackService.sendFeedbackRequestsForTrip(tripId);
    } catch (Exception ex) {
      log.error("Failed to send feedback requests for completed trip {}", tripId, ex);
    }

    try {
      tripRealtimePublisher.publishUpdatedByTripId(tripId);
    } catch (Exception ex) {
      log.error("Failed to publish trip completion update for trip {}", tripId, ex);
    }
  }
}

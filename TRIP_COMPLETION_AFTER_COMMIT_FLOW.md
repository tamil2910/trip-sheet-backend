# Trip Completion After-Commit Flow

`processAfterTripCompletion` now schedules its work with Spring transaction synchronization instead of running the side effects immediately.

## Why this change

The trip is first saved inside `dropTrip(...)` within the database transaction. The billing, feedback, and realtime publish steps should only run if that transaction commits successfully.

## Flow

1. `dropTrip(...)` updates the trip status to `COMPLETED`.
2. The trip is saved with `repository.save(trip)`.
3. `processAfterTripCompletion(completedTrip)` is called.
4. If a transaction is active, the method registers an `afterCommit` callback.
5. When the transaction commits, the callback runs:
   - `tripRealtimePublisher.publishUpdated(completedTrip)`
   - `tripBillingService.generatePurchaseOrdersForTrip(completedTrip)`
   - `tripFeedbackService.sendFeedbackRequestsForTrip(completedTrip)`
6. If the transaction rolls back, the callback is never executed.

## Result

The follow-up work is now tied to a successful commit, which prevents billing or feedback side effects from firing for rolled-back trip completions.

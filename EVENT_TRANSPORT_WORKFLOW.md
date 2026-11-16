# Event Transport Management

## Purpose

Provide an end-to-end workflow for organising event transport: event setup, invited passenger registration, eligibility approval, route grouping, trip creation, and temporary passenger access to trip details.

## Actors

- **Event organiser**: a place, vendor, organisation, or other authorised tenant that creates and manages the event.
- **Invitee / participant**: a recipient who receives an event registration link and submits their transport needs.
- **Approver**: an administrator or designated event contact who reviews participants that cannot be matched to an existing `PeopleTenant` record.
- **Transport coordinator**: reviews route suggestions, finalises groups, and assigns vehicles and duty types.
- **Passenger**: an approved participant who uses time-limited access to view their assigned trip.

## Event setup

An organiser creates an event with at least a name, event location, date/time, and registration and access deadlines. The organiser selects one or more requested vehicle types (for example, sedan and SUV). Each selected type must have a configured seating capacity. The event also defines the arrival time or required arrival window used to calculate pickup ETAs.

The organiser adds participants as invitees, individually or in bulk, with the available contact details. The system sends each invitee a unique registration link with an expiry time. The link is tied to the event and invitee, can be revoked, and must not grant access after expiry.

## Participant registration and approval

1. The invitee opens the valid link and submits the requested identity/contact details and pickup location. Pickup coordinates (`latitude`, `longitude`) are required for routing; an address label may also be collected for display and verification.
2. The system verifies the submitted phone number and/or email address against `PeopleTenant` records in the relevant organisation. Contact values should be normalised before matching. A match must be scoped to the event's organisation and must not be based on name alone.
3. If a matching `PeopleTenant` identity is found and the required contact is verified, the participant is automatically approved for the event. Store the link to the matched person record and the verification outcome.
4. If there is no match, conflicting match, or insufficient verification, set the application to **Pending approval**. An administrator or designated event contact can approve or reject it. Record who decided, when, and any reason.
5. Notify the participant of the outcome. Approval notification includes a separate time-limited access link for trip status and details. The registration link and access link are distinct credentials with separate purposes and expiries.

Suggested participant states: `INVITED`, `SUBMITTED`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `WITHDRAWN`, `EXPIRED`.

## Route grouping and pickup estimates

After registration closes (or when the coordinator chooses to run planning), the system generates proposed vehicle groups from approved participants with valid coordinates.

- Use pickup coordinates, event destination, required arrival time, road-route distance and travel-time estimates to form practical route clusters. Straight-line distance alone is not sufficient for final ETA planning.
- Respect each group's assigned vehicle type and its configured seating capacity. Do not exceed capacity. For example, ten people may be split into a six-seat SUV group and a four-seat sedan group if those capacities and vehicle availability apply.
- Where a group has multiple pickups, order stops using route estimates and calculate an ETA and suggested pickup time for each participant, working backwards from the event arrival time and including configured buffers.
- Keep participants unassigned when capacity, data quality, or routing constraints prevent a reliable assignment. Surface the reason for coordinator action.
- Treat generated groups as proposals. The coordinator can move or remove participants, add people to another group, change group vehicle type, and adjust pickup times. Recalculate affected route estimates after edits and show the impact before confirmation.

A group should retain its participants, ordered pickup stops, vehicle type, seat capacity, per-person pickup time/ETA, route estimate, planning version, and status. A coordinator's manual edits must be retained as the confirmed plan and not silently overwritten by a later recalculation.

## Vehicle assignment and trip creation

For each confirmed group, the coordinator assigns a specific vehicle and a duty type. The group can carry an explicit `createTrip` flag:

- When `createTrip` is false, save the group and assignments as an event transport plan without creating a trip.
- When `createTrip` is true and the group has a vehicle type, duty type, valid passengers, and required pickup/drop-off details, create one trip for that group using the existing trip workflow.
- Link the created trip to the event and include the event name in the trip's display/reference information. Include the group's passengers and ordered pickup stops, with each passenger's pickup time where supported.
- Make trip creation idempotent for a group so retrying confirmation cannot create duplicate trips. Report validation failures per group and leave the failed group available for correction.

The coordinator can see trip creation status and the linked trip identifier for each group. Changes after trip creation must follow the existing trip update/cancellation rules and preserve an audit trail.

## Temporary passenger access and OTP login

Approved participants receive an email containing a time-limited access link. The link identifies the event access request, not a reusable password. During the configured access window, the participant can authenticate using their registered phone number and a one-time passcode (OTP). Email can be used as a supported verification or recovery channel where configured; OTP delivery must be verified before access is granted.

After authentication, the participant can view only their own event registration, group assignment, pickup time/location, vehicle details when available, trip status, and trip information. They cannot browse other participants' personal data. Access expires at the configured end time, normally the event's end plus any organiser-configured grace period. Expired or revoked links and sessions cannot be used to sign in or retrieve trip details. OTPs are short-lived, single-use, rate-limited, and never stored or logged in plaintext.

## Notifications

Send notifications for invitation, registration receipt, pending review, approval or rejection, access-link issuance, group/pickup changes, vehicle or trip assignment, and material trip status updates. Include the event name and relevant deadlines. Avoid exposing other participants' details in messages.

## Core records and relationships

A first implementation will likely need records corresponding to:

- **Event**: organiser/tenant, event details, arrival requirement, registration deadline, access expiry, lifecycle status.
- **Event vehicle type**: event, vehicle type, configured capacity or capacity snapshot, requested count if applicable.
- **Invite**: event, recipient contact, secure token hash, expiry, revoked/used state.
- **Participant registration**: event, submitted details, pickup coordinates/address, matched `PeopleTenant`, verification result, approval state and decision audit.
- **Event group**: event, vehicle type/capacity, ordered participant stops, ETA and pickup times, planning/confirmation state.
- **Vehicle assignment**: group, actual vehicle, duty type.
- **Trip link**: group and created trip, creation status/idempotency key.
- **Temporary access grant/session**: participant, event, expiry/revocation, authentication state.

These are conceptual entities, not a claim that matching tables or APIs already exist. Reuse existing `PeopleTenant`, vehicle type, duty type, passenger route, and trip models where their current constraints fit. Keep event-specific application and grouping state separate from the canonical person and trip records.

## Security, privacy, and audit requirements

- Use cryptographically random tokens; store hashes rather than raw invite/access tokens. Enforce expiry and revocation server-side.
- Verify contact ownership with OTP before treating a phone/email match as proof of identity. Match only within the correct organisation/event scope and handle duplicates as manual review.
- Validate coordinate ranges and require explicit consent/notice for collecting pickup location. Restrict location visibility to authorised event/transport staff and the participant.
- Apply role and tenant checks to every organiser, approver, and coordinator action. Participants may access only their own records and trips.
- Audit approval decisions, route-plan edits, vehicle assignments, trip creation, link revocations, and access expiry.
- Define retention and deletion rules for event registrations, OTP metadata, links, and pickup locations.

## Acceptance criteria

1. An authorised organiser can create an event and select multiple vehicle types with capacities and registration/access deadlines.
2. Invitees receive event-specific links that stop working at expiry or revocation.
3. A submitted registration can match a `PeopleTenant` by verified phone or email within the correct organisation; eligible matches are auto-approved and unresolved cases enter manual review.
4. Approved participants have valid pickup coordinates and can be grouped without exceeding each group's capacity.
5. The route plan provides ordered stops and per-person pickup ETAs/times and can be manually adjusted before confirmation.
6. A coordinator can assign each group a vehicle and duty type; `createTrip: true` creates one linked trip with the event name, while false creates none.
7. Retrying group confirmation does not duplicate its trip.
8. Participants can use OTP and a valid access grant to view only their own trip information; access ends at expiry or revocation.
9. Approval, planning, assignment, and trip actions are auditable, and participants receive relevant status/change notifications.

## Open product decisions

- Whether invitees are always preloaded by the organiser or can self-register from a shareable event link.
- Whether matching requires both verified phone and email when both are supplied, or accepts either verified identifier.
- Whether vehicle counts are requested per type or selected dynamically by the routing planner.
- The route provider, acceptable ETA accuracy, pickup buffer, and behaviour when estimates are unavailable.
- Whether passenger approval is required before final group confirmation, and how late edits affect an already-created trip.
- The access window's default end and grace period, plus supported OTP channels and fallback when delivery fails.
- Event cancellation, participant withdrawal, no-show, and post-event data retention policies.

## Existing codebase context

The repository already contains `PeopleTenant`, passenger route analysis/confirmation services, vehicle and duty type concepts, trip creation, and guest passenger trip access. This document describes the desired event-level workflow and the additional lifecycle and temporary-access rules needed around those concepts; it is not an implementation inventory or API contract.

## Supplier, vehicle, and driver availability

Event transport resources can be sourced through suppliers, with different rules depending on who manages the event.

### Organisation-managed event

An organisation that created the event can add vendors as suppliers for that event. The organisation's authorised event coordinator can view and manage those vendor relationships and the availability submissions that the vendors provide for the event.

### Vendor-managed or vendor-assigned event

A vendor that created the event, or has been assigned responsibility for it, can add its own suppliers to support event transport. These vendor-added suppliers and their event-specific submissions are private to that vendor's event operations. They must not appear in the organisation's supplier list or organisation-facing event views. The organisation may see the final event transport assignments and trip details needed to manage the event, subject to its role, but not the vendor's private supplier roster unless a separate explicit sharing rule is introduced.

Enforce these visibility rules on the server for list, detail, search, export, and notification operations. Hiding supplier data only in the user interface is insufficient. A vendor may manage only suppliers it is authorised to use, and an assigned vendor's scope is limited to the event(s) assigned to it.

### Supplier invitation and availability submission

1. An authorised organisation or vendor adds a supplier to the event and specifies the requested service window and any required vehicle types, capacities, quantities, driver requirements, and submission deadline.
2. The system emails the supplier a unique, time-limited event supplier link. The link is scoped to that supplier and event, can be revoked, and grants access only to the availability submission flow.
3. The supplier opens the link and lists the vehicles and drivers it can make available for the event. Vehicle entries should reference the supplier's registered vehicles where possible and include vehicle type, seating capacity, registration/identifier, and availability window. Driver entries should reference the supplier's registered drivers and include availability window and required assignment details.
4. The supplier submits its offer. The vendor or organisation managing the supplier relationship reviews and confirms the submitted availability. A submission is not an assignment by itself.
5. The supplier can update or withdraw availability before the cutoff. Changes after a vehicle or driver has been assigned must be flagged for coordinator review and must not silently invalidate an active trip.

Suggested supplier states: `INVITED`, `LINK_OPENED`, `SUBMITTED`, `UNDER_REVIEW`, `CONFIRMED`, `DECLINED`, `WITHDRAWN`, `EXPIRED`.

### Event allocation rules

The vendor can allocate event groups only from vehicles and drivers that are both (a) registered/authorised under that vendor and (b) confirmed as available for the event and relevant service window. The vehicle must satisfy the group's vehicle type and seating capacity; the driver must be available and eligible for the duty. Do not allow assignment of an unsubmitted, declined, withdrawn, expired, or conflicting resource. If a resource becomes unavailable after assignment, mark the assignment for replacement and notify the coordinator.

Where the event's managing party is an organisation, the organisation's authorised coordinator can review vendor offers and choose from confirmed resources according to the agreed workflow. Where a vendor manages the event or has operational assignment responsibility, the vendor chooses from its own confirmed supplier resources. The final vehicle and driver assignment remains linked to the event group and, when `createTrip` is true, is reflected in the created trip using the existing vehicle/driver assignment rules.

## Additional records and audit

Extend the conceptual record list with:

- **Event supplier relationship**: event, supplier, inviting/managing tenant, scope/visibility, invitation state, requested services, and submission deadline.
- **Supplier invitation**: event, supplier, secure token hash, expiry, revocation and submission state.
- **Supplier vehicle availability**: event supplier relationship, supplier vehicle, type/capacity snapshot, service window, availability state, review state.
- **Supplier driver availability**: event supplier relationship, supplier driver, service window, availability state, review state.
- **Event resource assignment**: event group, selected confirmed vehicle and driver, assigning vendor/coordinator, assignment state, and change history.

Audit supplier invitations, submissions, edits, withdrawals, review decisions, visibility changes, assignments, and replacements. Notifications to a supplier must expose only that supplier's own request and submission. Organisation and vendor notifications must follow the same event visibility rules as the corresponding screens.

## Additional acceptance criteria

10. An organisation event manager can add vendors as event suppliers and review their event availability.
11. A vendor that created or was assigned an event can add its own suppliers; those supplier records and offers are not visible in organisation supplier lists or APIs.
12. Each supplier receives an event-scoped email link with expiry/revocation and can submit available vehicles and drivers for the requested event window.
13. Suppliers can update or withdraw offers before the cutoff, and post-assignment changes trigger coordinator review and notification.
14. A vendor can assign only its own authorised vehicles/drivers that are confirmed available for that event and service window; invalid or unavailable resources are rejected.
15. The organisation/vendor visibility rules are enforced by backend authorization for every relevant read and write path.

## Temporary participant login without `UserAccount`

Event participants are temporary event users, not permanent application users. Do not create a `UserAccount` just to provide event access. Keep the participant's submitted identity and contact information on the event registration and link it to `PeopleTenant` only when an appropriate existing person record is verified. A matched `PeopleTenant` record remains the canonical person record; it does not imply creation of an application login.

Use an event-scoped access grant linked to the approved registration:

1. After approval, issue a random, high-entropy, single-use link by email. Store a hash of the token, its event/registration scope, expiry, and revocation/use state.
2. On link redemption, verify control of the registered contact using a short-lived OTP. The link alone and knowledge of a phone/email address must not expose trip details.
3. On successful verification, issue a short-lived session whose subject is the event access grant/registration, not a `UserAccount`. Recheck grant expiry, revocation, event scope, and registration state on every request.
4. Permit access only to that participant's own registration, pickup details, group assignment, and event trip information. Do not grant general application roles or access to other event participants.
5. Expire and revoke sessions at the configured access end time (normally event end plus an organiser-configured grace period), on explicit revocation, or when the participant is withdrawn/rejected. Retain event/trip operational records according to the retention policy; access expiry does not delete them.

Suggested records are `EventParticipantAccess`/`EventAccessGrant`, OTP challenge metadata, and temporary session/token state, all linked to the registration. OTPs must be single-use, short-lived, rate-limited, and never stored or logged in plaintext. Existing application users may continue using normal account access where permitted, but the event link must remain temporary and event-scoped.

## Temporary driver access from a mobile browser

A driver who is assigned to an event trip should be able to complete the driver trip workflow in a mobile browser without installing or signing into the driver app. Do not create a permanent `UserAccount` solely for this temporary event access. Link access to the existing driver profile and a specific event trip assignment. Send the assigned driver a time-limited link to the driver's registered email after assignment; reassigning the trip must revoke the former driver's access and issue access to the newly assigned driver.

### Driver link and session flow

1. When a driver is assigned to an event trip, send an email with a high-entropy, event/trip-scoped link and expiry. Store only a token hash and support revocation. The driver link must not be usable to view unassigned trips or another driver's work.
2. Redeem the link and verify the driver's email with a short-lived OTP (or an equivalent verified challenge). Successful verification creates a temporary session bound to the existing driver profile, assignment, and allowed event trip actions, without a `UserAccount` principal.
3. Open a responsive, mobile-browser interface. It must support the applicable `DriverTripController` workflow: view assigned trips; view trip details; update permitted trip details; dispatch the trip; mark arrival; start the trip; and end/complete the trip. The create-trip operation is not part of event assignment access unless a separate policy explicitly enables it; event trips are created and assigned by the coordinator workflow.
4. Enforce the same trip state transitions, required data, validations, and restrictions as the normal driver controller. In particular, actions must be permitted only for the driver currently assigned to the trip. The browser flow is an alternate client for driver operations, not a privilege bypass.
5. Expire access at the configured event/assignment end time or after trip completion plus a configured grace period. Revoke immediately on reassignment, cancellation, driver deactivation, or manual revocation. If a driver has multiple event trips, scope the grant to the listed assignments and expire it when all included assignments are no longer actionable.
6. Record the acting driver profile, trip, action, and timestamp for all state changes. Protect the session against replay and theft; do not put long-lived bearer credentials in URLs after redemption, and do not expose trip access tokens in logs or referrers.

A driver access grant/session should reference the driver profile and assignment, not a `UserAccount`. The existing `DriverTripController` currently derives the driver from an authenticated `UserAccount`; implementing the temporary browser flow therefore needs an event-driver authentication path or controller/service adapter that supplies a verified driver identity and applies the same service-level authorization and validation. Avoid fabricating a `UserAccount` just to satisfy that controller's current request context.

The browser should expose driver-relevant operational information for the assigned trip, such as pickup sequence, pickup times/locations, passenger count and necessary contact/instructions, event destination, assigned vehicle, duty type, and current trip status. Apply data minimisation: show personal/contact details only when needed to perform the assigned duty, and never expose unrelated participants or supplier records.

## Additional conceptual records

- **Event driver access grant**: driver profile, event trip/assignment scope, token hash, email challenge/verification state, expiry, revocation, and redemption state.
- **Temporary access session**: grant, session/token hash or identifier, creation/expiry, revocation, and last activity; no `UserAccount` foreign key is required for a temporary principal.
- **Access action audit**: temporary principal type and referenced participant registration or driver profile, event/trip, action, timestamp, and outcome.

## Additional acceptance criteria

16. Event participant access works without creating a `UserAccount`; a verified `PeopleTenant` match can be linked without creating an application login.
17. Participant links and sessions are scoped to one registration and expose only that participant's information; expiry and revocation are enforced server-side.
18. Assigning an event trip to a driver sends a time-limited link to the driver's registered email, and the driver can authenticate and use the driver workflow from a mobile browser without the app.
19. Temporary driver access supports applicable trip list/detail, update, dispatch, arrival, start, and completion actions with the same validation and state rules as the regular driver flow; trip creation remains coordinator-driven for event trips.
20. A temporary driver session can act only on trips currently assigned to that driver, and reassignment/cancellation/completion causes the configured access revocation or expiry behavior.
21. Temporary participant and driver access actions are auditable, and neither flow creates a `UserAccount` solely to support the temporary session.

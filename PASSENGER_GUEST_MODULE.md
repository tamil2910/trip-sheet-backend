# Passenger (Guest Role) Module Implementation

This document outlines the requirements and API structure for the Passenger module, specifically for users with the **GUEST** role.

## 1. Backend Components

### Model
- **PassengerOrganisationTenantMapping**: Links the `UserAccount` (the Guest) to multiple `Tenant` (Organisations).

### Repository
- **PassengerOrganisationTenantMappingRepository**: Manages the `passenger_organisation_tenant_mappings` table.

### Service
- **PassengerService**: Interface defining the contract for passenger-related operations.
- **PassengerServiceImp**: Implementation of `PassengerService`, located in `services/PassengerService/`.
    - Implements logic for organization search and linking.
    - Implements logic for filtering and managing trips for `GUEST` users.

### Controller
- **PassengerController**: Located in `controllers/`. Handles all Guest-facing endpoints.

## 2. Organization Discovery & Linking

### Search Organisation
- **Endpoint**: `GET /passengers/organisation/search`
- **Param**: `uniqueCode` (String)
- **Description**: Allows a Guest to search for an organization using its unique code.
- **Response**: `Tenant`
```json
{
  "success": true,
  "message": "Organisation found",
  "data": {
    "id": "uuid",
    "tenantName": "ABC Travels",
    "contactEmail": "ops@abctravels.com",
    "tenantUniqueCode": "ABCT1234",
    "tenantType": "ORGANISATION",
    "isActive": true
  }
}
```

### Link Organisation
- **Endpoint**: `POST /passengers/organisation/link`
- **Body**: `{ "uniqueCode": "String" }`
- **Logic**:
    1. Validates the organization unique code.
    2. Creates a mapping in `passenger_organisation_tenant_mappings`.
    3. Updates the `UserAccount.tenant` to the newly linked organization.
- **Response**: success message only
```json
{
  "success": true,
  "message": "Organisation linked successfully",
  "data": null
}
```

## 3. Trip Management (Passenger Perspective)

Guests can only manage trips they are involved in.

### API Endpoints

### 3.1 Create Trip
- **Method**: `POST`
- **Endpoint**: `/passengers/trips`
- **Description**: Request a new trip for the guest's current organisation.
- **Rules**:
  - `isManualTrip` is forced to `false`
  - `organisationId` is ignored from payload and replaced by the linked guest organisation

**Request payload**
```json
{
  "tripType": "SINGLE",
  "parentTripId": null,
  "recurrenceInterval": null,
  "daysOfWeek": null,
  "recurrenceFrequency": null,
  "vendorId": null,
  "organisationId": null,
  "driverId": null,
  "vehicleId": null,
  "dutyTypeId": "uuid",
  "vehicleTypeId": "uuid",
  "passengerIds": ["uuid"],
  "bookerId": "uuid",
  "passengerCustomFieldValues": [],
  "pickupTime": 1718524800,
  "startDate": null,
  "endDate": null,
  "notes": "Airport pickup",
  "isManualTrip": false,
  "stops": [
    {
      "sequenceNumber": 1,
      "stopType": "PICKUP",
      "addressText": "Airport Terminal 1",
      "formattedAddress": "Airport Terminal 1",
      "latitude": 12.97,
      "longitude": 77.59,
      "accurate": true
    }
  ]
}
```

**Response payload**
```json
{
  "success": true,
  "message": "Trip request created",
  "data": {
    "id": "uuid",
    "tripCode": "12345678",
    "tripStatus": "CREATED",
    "tripType": "SINGLE"
  }
}
```

### 3.2 View/Search Trips
- **Method**: `GET`
- **Endpoint**: `/passengers/trips`
- **Description**: Lists trips where the guest is the booker or a passenger, within the currently linked organisation.
- **Common query params**:
  - `page`
  - `size`
  - `sort`
  - `searchValue`
  - `tripStatus`
  - `tripType`
  - `startDate`
  - `endDate`
  - `onlyCreated`

**Response payload**
```json
{
  "success": true,
  "message": "Trips fetched",
  "data": {
    "data": [],
    "currentPage": 0,
    "pageSize": 10,
    "currentPageCount": 0,
    "totalItems": 0,
    "totalPages": 0,
    "isFirst": true,
    "isLast": true,
    "hasNext": false,
    "hasPrevious": false,
    "page": 0,
    "size": 10
  }
}
```

### 3.3 Get Trip
- **Method**: `GET`
- **Endpoint**: `/passengers/trips/{id}`
- **Description**: Fetch details of a specific trip if the guest is involved in it.
- **Path param**: `id` (UUID)

**Response payload**
```json
{
  "success": true,
  "message": "Trip details fetched",
  "data": {
    "id": "uuid",
    "tripCode": "12345678",
    "tripStatus": "CREATED",
    "tripType": "SINGLE"
  }
}
```

### 3.4 Update Trip
- **Method**: `PUT`
- **Endpoint**: `/passengers/trips/{id}`
- **Description**: Updates a trip only when:
  - the trip is in `CREATED` status
  - the guest is the booker
- **Path param**: `id` (UUID)

**Request payload**
```json
{
  "tripType": "SINGLE",
  "organisationId": null,
  "vendorId": null,
  "driverId": null,
  "vehicleId": null,
  "dutyTypeId": "uuid",
  "vehicleTypeId": "uuid",
  "passengerIds": ["uuid"],
  "bookerId": "uuid",
  "passengerCustomFieldValues": [],
  "pickupTime": 1718524800,
  "startDate": null,
  "endDate": null,
  "notes": "Updated note",
  "isManualTrip": false,
  "stops": []
}
```

**Response payload**
```json
{
  "success": true,
  "message": "Trip updated successfully",
  "data": {
    "id": "uuid",
    "tripCode": "12345678",
    "tripStatus": "CREATED",
    "tripType": "SINGLE"
  }
}
```

### 3.5 Delete Trip
- **Method**: `DELETE`
- **Endpoint**: `/passengers/trips/{id}`
- **Description**: Cancels a trip only when it is in `CREATED` status.
- **Path param**: `id` (UUID)

**Response payload**
```json
{
  "success": true,
  "message": "Trip cancelled successfully",
  "data": null
}
```

## 4. Role & Permissions: GUEST

Endpoints are restricted to the `GUEST` role via Spring Security.

## 5. Implementation Notes

- **Security**: All endpoints must be secured with `@PreAuthorize("hasRole('GUEST')")`.
- **Isolation**: Guests can only see trips within the organization currently set in their `UserAccount`.
- **List response shape**: The trip list endpoint returns a paginated wrapper object with `data`, `currentPage`, `pageSize`, `currentPageCount`, `totalItems`, `totalPages`, `isFirst`, `isLast`, `hasNext`, `hasPrevious`, `page`, and `size`.
- **Mapping table**: `passenger_organisation_tenant_mappings` should keep one row per `(user_account_id, organisation_id)` pair.

---
*Created: 2026-06-16*

# Android Studio Prompt: Galaxy Watch 9 to Health Connect to Cloud Bridge

Build an Android phone application that reads health data synchronized from a Samsung Galaxy Watch 9, reads the permitted records through Android Health Connect, maps them to the cloud API contract, and uploads them to the Health Digital Twin backend.

The application must run on the Galaxy phone. Do not assume that the watch directly connects to the cloud. The intended path is:

```text
Samsung Galaxy Watch 9
        ↓
Samsung Health on Galaxy Phone
        ↓
Android Health Connect
        ↓
Android bridge application
        ↓ HTTPS
Health Digital Twin REST API
        ↓
Cloud database and dashboard
```

## Important source and integration constraints

1. The watch is the measurement device, but Samsung Health on the phone is responsible for synchronization.
2. The bridge must use explicit user consent and Health Connect permissions.
3. Do not claim that a record came directly from the watch unless the source metadata proves it.
4. The bridge must distinguish:
   - a value received from Health Connect,
   - a value unavailable because permission was not granted,
   - a value unavailable because no record exists,
   - a value not supported by this bridge.
5. Do not invent, interpolate, or fill missing health readings.
6. The app is for fitness/wellness data integration. Do not present it as a medical diagnostic application.
7. Prefer Health Connect APIs for the bridge. Samsung's older Samsung Health Android SDK is deprecated; use the current Samsung Health Data SDK only if a required Samsung-specific data type cannot be obtained through Health Connect.
8. The cloud URL must be configurable. Never hardcode production credentials, API keys, or a deployment URL.

## Current cloud API

The current Spring Boot backend is available at a configurable base URL:

```text
https://<cloud-host>
```

For local development:

```text
http://<computer-lan-ip>:8080
```

The phone and development computer must be on the same network for local HTTP testing. Production must use HTTPS.

### POST health data

```http
POST /api/health-records
Content-Type: application/json
```

Full local example:

```text
http://192.168.1.25:8080/api/health-records
```

Required request body:

```json
{
  "userId": "user_samsung_watch9_twin",
  "platform": "Samsung Galaxy Watch 9 & Galaxy Phone",
  "timestamp": 1793200661908,
  "healthParameters": {
    "steps": 1250,
    "calories": 180.5,
    "distance": 950.0,
    "spo2": 98.0,
    "temperature": 36.6,
    "syncCount": 5
  }
}
```

Field contract:

| Field | Type | Required | Meaning |
|---|---:|:---:|---|
| `userId` | string | yes | Stable application user identifier |
| `platform` | string | yes | Source description |
| `timestamp` | long | yes | Reading/event time as Unix epoch milliseconds |
| `healthParameters.steps` | integer | yes | Step count |
| `healthParameters.calories` | number | yes | Current collector's calorie value; document whether mapped from active calories |
| `healthParameters.distance` | number | yes | Distance in meters |
| `healthParameters.spo2` | number | yes | Oxygen saturation percentage |
| `healthParameters.temperature` | number | yes | Temperature in degrees Celsius; identify source/method in bridge logs and UI metadata |
| `healthParameters.syncCount` | integer | yes | Bridge synchronization counter |

The current backend requires all six `healthParameters` values. If a Health Connect permission is missing or a value is unavailable, do not send a fabricated zero. The bridge must either:

1. hold the record and retry when all required values are available, or
2. report a clear “cannot upload because required fields are missing” state.

Do not silently substitute `0`, the last value, or a guessed value.

Successful response:

```json
{
  "success": true,
  "message": "Health data received successfully",
  "userId": "user_samsung_watch9_twin",
  "timestamp": 1793200661908
}
```

Validation failure response:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "healthParameters.steps": "steps is required"
  }
}
```

The server deduplicates identical retries for the same user and measurement payload. The bridge should still maintain its own local outbox and stable event identity so that retries are safe.

### Read endpoints

These endpoints are useful for diagnostics and bridge verification:

```http
GET /api/health-records/status
GET /api/health-records/latest?userId=<userId>
GET /api/health-records?userId=<userId>&page=0&size=20
GET /api/health-records/history?userId=<userId>&hours=24
GET /api/health-records/summary?userId=<userId>
```

The bridge does not need to poll these endpoints for normal uploading. The dashboard uses them for display.

### Live dashboard endpoint

```http
GET /api/health-records/events
```

This is a Server-Sent Events stream for browsers. The Android bridge does not need to consume it.

## Android application requirements

### Project

Create a native Android application in Android Studio using:

- Kotlin
- Jetpack Compose or a clean XML UI
- MVVM or another clearly separated architecture
- Repository layer
- WorkManager for reliable background synchronization
- Kotlin coroutines
- Retrofit or another typed HTTP client
- Room for local Health Connect cursor/state and upload outbox
- Android Health Connect client APIs

Use the latest stable versions compatible with the selected compile SDK. Keep dependency versions centralized.

### Screens

Implement these screens:

1. **Onboarding**
   - Explain the data flow.
   - Explain that Samsung Health must first synchronize the Galaxy Watch data to the phone.
   - Explain that Health Connect permissions are required.
   - Explain that the app uploads wellness data to the configured cloud backend.

2. **Permission and connection setup**
   - Check whether Health Connect is installed and available.
   - Request only the permissions required by the enabled data types.
   - Show granted and denied permissions individually.
   - Provide a link/instruction to enable Samsung Health synchronization with Health Connect.
   - Show the configured API environment without exposing secrets.

3. **Sync dashboard**
   - Last Health Connect read time.
   - Last successful cloud upload time.
   - Pending outbox count.
   - Last error.
   - Current connection state: ready, permission required, waiting for data, offline, retrying, or uploaded.
   - Counts by data type.

4. **Data preview**
   - Show the latest records before upload.
   - Show source, record time, unit, and Health Connect record type.
   - Clearly identify missing values.

5. **Settings**
   - User ID.
   - Cloud base URL.
   - Sync frequency.
   - Wi-Fi-only option.
   - Manual “Sync now”.
   - Clear local outbox.
   - Revoke/reconfigure permissions.

## Health Connect data mapping

Read only types for which the user granted permission and the bridge has a defined mapping.

Recommended initial read set:

- Steps
- Distance
- Active calories burned
- Heart rate
- Oxygen saturation
- Body temperature or skin temperature, only if available from the source
- Exercise sessions
- Sleep sessions and sleep stages

Possible later extensions:

- Resting heart rate
- Heart-rate variability
- Blood pressure
- Body composition
- Weight and height
- Floors climbed
- Nutrition
- Water intake
- Sleep apnea records
- Energy score or other Samsung Health-derived summaries

Do not assume every Galaxy Watch 9, country, Samsung Health version, or Health Connect provider exposes every type. The app must detect availability at runtime.

### Initial payload mapping

Map the first complete Health Connect sample into the current cloud contract:

| Health Connect data | Cloud field | Rules |
|---|---|---|
| Steps record | `healthParameters.steps` | Use the interval ending at or nearest to the upload event; avoid double counting |
| Active calories burned | `healthParameters.calories` | Use active calories, not total calories; document this choice in the app |
| Distance record | `healthParameters.distance` | Convert meters to the cloud unit: meters |
| Oxygen saturation | `healthParameters.spo2` | Use percentage, not fraction |
| Body/skin temperature | `healthParameters.temperature` | Convert to Celsius; preserve source type in local metadata |
| Sync operation count | `healthParameters.syncCount` | Increment only after a successful synchronization cycle or use a persisted monotonic counter |

The current API does not accept heart rate, sleep, exercise, HRV, blood pressure, body composition, nutrition, or ECG fields. Do not discard these silently. Store them in the local Room outbox or a typed extension model and expose them in the bridge's data preview/log, then add a versioned backend contract before uploading them.

## Synchronization design

Implement both:

### Manual synchronization

When the user taps **Sync now**:

1. Verify Health Connect availability.
2. Verify required permissions.
3. Query records since the last successful cursor/time.
4. Normalize units and timestamps.
5. Aggregate records into the current required cloud payload.
6. Validate that all required fields are present.
7. Save a pending upload to Room before making the network request.
8. Upload to `POST /api/health-records`.
9. Mark the outbox item as uploaded only after a successful 2xx response.
10. Advance the Health Connect cursor only after the upload is durable.
11. Update the UI status and audit metadata.

### Background synchronization

Use WorkManager with network constraints. The worker must:

- Survive process death and device restart.
- Use exponential backoff.
- Avoid uploading duplicate records.
- Respect battery constraints.
- Avoid excessive Health Connect reads.
- Keep the outbox bounded and observable.
- Never log raw health values unnecessarily.

Use a unique periodic work name so multiple workers are not scheduled accidentally.

## Reliability and error handling

Handle these cases explicitly:

- Health Connect unavailable.
- Samsung Health has not synchronized the watch.
- Permission denied or revoked.
- No new records.
- Required metric missing.
- Phone offline.
- DNS failure.
- TLS failure.
- HTTP 400 validation response.
- HTTP 401/403 authentication failure if cloud auth is added.
- HTTP 409 duplicate/conflict response.
- HTTP 429 rate limiting.
- HTTP 5xx server failure.
- App process killed during upload.
- Clock/time-zone changes.

Use a stable local event key based on:

```text
userId + sourceRecordId + recordType + startTime + endTime
```

Do not use the current wall-clock time alone as the record identity.

## Security requirements

- Production API traffic must use HTTPS.
- Do not enable cleartext traffic except in a clearly marked local-development build.
- Do not store cloud secrets in the APK.
- Store user identifiers and tokens using Android Keystore-backed storage where applicable.
- Request minimum Health Connect permissions.
- Explain each permission before requesting it.
- Do not log health values, access tokens, or complete request bodies in production.
- Add authentication to the cloud API before exposing it publicly.
- Use certificate validation and avoid disabling TLS checks.

## Acceptance tests

Implement tests for:

1. Health Connect availability detection.
2. Permission state handling.
3. Mapping steps to integer steps.
4. Mapping active calories, not total calories.
5. Meter-to-meter distance mapping.
6. SpO₂ percentage mapping.
7. Temperature conversion to Celsius.
8. Missing required data blocks upload without fabricated values.
9. Stable event ID generation.
10. Successful POST to `/api/health-records`.
11. HTTP validation failure handling.
12. Offline upload retained in Room and retried.
13. Process death during upload does not lose the outbox item.
14. Duplicate retry does not create an unbounded local queue.
15. Manual sync and WorkManager sync do not run concurrently for the same user.
16. User data is separated by `userId`.

## Android Studio deliverables

Provide:

- Source code for the bridge app.
- A `README.md` with setup and permission instructions.
- A configurable local/staging/production API base URL.
- A sample local API configuration using `http://<computer-lan-ip>:8080`.
- Health Connect permission declarations.
- Room schema and migration strategy.
- WorkManager sync worker.
- Retrofit API interface.
- Unit tests and instrumentation tests.
- A mapping table documenting every Health Connect record type.
- A troubleshooting guide for Samsung Health synchronization.

Do not modify the cloud API contract without documenting the change. If the current required six-field payload prevents uploading partial but valid Health Connect data, propose a versioned extension such as:

```text
POST /api/v2/health-events
```

with typed event records. Keep `/api/health-records` compatible until the backend migration is approved.

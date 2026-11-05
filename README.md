# Real-Time Health Digital Twin Dashboard

This project implements a complete local prototype of the Samsung Galaxy Watch 9 digital-twin dashboard described in the prompt. It includes a Spring Boot backend, H2 database persistence, and a Vite React frontend.

## Architecture

- Backend: Spring Boot 3.3.5 + Java 17 + Maven
- Frontend: React + Vite + Recharts
- Database: H2 in file-based mode for local development
- Real-time updates: Server-Sent Events (SSE) from the backend

## Local backend setup

```powershell
cd backend
mvn spring-boot:run
```

The backend runs on `http://localhost:8080`.

## Local frontend setup

```powershell
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

## API endpoints

### Submit health data

```powershell
$body = Get-Content .\sample-health-data.json -Raw
Invoke-RestMethod -Uri "http://localhost:8080/api/health-records" -Method Post -ContentType "application/json" -Body $body
```

### Latest reading

```powershell
Invoke-RestMethod "http://localhost:8080/api/health-records/latest?userId=user_samsung_watch9_twin"
```

### History

```powershell
Invoke-RestMethod "http://localhost:8080/api/health-records/history?userId=user_samsung_watch9_twin&hours=24"
```

### Summary

```powershell
Invoke-RestMethod "http://localhost:8080/api/health-records/summary?userId=user_samsung_watch9_twin"
```

### SSE stream

```powershell
Invoke-WebRequest "http://localhost:8080/api/health-records/events" -Method Get
```

## Data format

The accepted payload mirrors the phone payload:

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

The timestamp is treated as epoch milliseconds and converted to local time for display. The sample data is explicitly demo data and not a real person’s current readings.

## ECG UI status

The dashboard includes an ECG panel, but the current phone payload does not contain an ECG signal. The UI therefore shows an explicitly labeled unavailable state rather than inventing a waveform or health measurement. A real ECG view requires an agreed API field and signal format from the mobile collector, plus backend validation and persistence for that data.

## Samsung Health and Health Connect parameter coverage

The UI separates signals that are already in the current collector payload from signals that require a mobile-collector/API change:

The parameter inventory is based on the official [Samsung Health Data SDK overview](https://developer.samsung.com/health/data/overview.html) and Android's [Health Connect overview](https://developer.android.com/health-and-fitness/health-connect). Samsung's SDK documentation explicitly requires user consent and per-data-type permissions.

- Activity: steps, walking/running distance, active calories, and exercise sessions.
- Heart and vitals: heart rate, resting heart rate, HRV where supported, SpO2, skin/body temperature, and irregular-heart-rhythm notifications.
- Sleep and recovery: sleep duration, sleep stages, Samsung Health sleep score, sleep apnea records where available, sleep goals, stress, and energy score.
- Body measurements: body composition, blood pressure, blood glucose records, weight, height, and floors climbed.
- Nutrition and goals: nutrition, water intake, active-calorie goals, active-time goals, step goals, and related Samsung Health targets.

The Galaxy Watch sensor hardware and Samsung Health app can support more measurements than every integration can export. Actual availability depends on watch sensor support, Samsung Health permissions, Health Connect permissions, Samsung Health sync settings, country/region restrictions, calibration requirements, and whether the phone collector explicitly reads and posts each record. The current backend contract still receives only steps, calories, distance, SpO2, temperature, and sync count, so the new catalog marks all other values as `Collector needed` instead of displaying invented readings. A raw ECG waveform is not treated as an ordinary Samsung Health Data SDK/Health Connect field here; the UI labels it as not exposed by this collector.

## Database

This project uses an H2 file database at `backend/data/healthdb.mv.db` by default. It is persistent across restarts when the same project directory is used. For production, replace the datasource configuration with a managed database such as PostgreSQL or MySQL and use environment variables.

## Real-time behavior

The dashboard polls the latest/history endpoints every 5 seconds and also opens a server-sent events stream to listen for new records. When a new valid payload is received, the REST API stores it and emits the event so the dashboard updates automatically without a refresh.

## Security notes

- CORS is restricted to `http://localhost:5173` and `http://127.0.0.1:5173`.
- Database credentials are not hardcoded in source.
- The endpoint should not be exposed publicly without authentication or network controls.

## Troubleshooting

- If the frontend cannot reach the backend, make sure the backend is running on port 8080.
- If records are not updating, check the database and make sure no H2 lock prevents disk writes.
- If the app is using a stale state, restart both backend and frontend processes.

## Cloud deployment prerequisites

- Configure a managed PostgreSQL or MySQL instance.
- Set environment variables for the datasource and enable TLS.
- Restrict access to the API with authentication, private networking, and a reverse proxy.
- Use a production frontend build and deploy it behind a web server or cloud static host.

## Android bridge prompt

The full Android Studio implementation prompt for building the Galaxy Watch 9 → Samsung Health → Health Connect → cloud bridge is in [ANDROID_HEALTH_CONNECT_BRIDGE_PROMPT.md](./ANDROID_HEALTH_CONNECT_BRIDGE_PROMPT.md). It documents the current API contract, payload mapping, permissions, WorkManager retry behavior, local outbox, security requirements, and acceptance tests.

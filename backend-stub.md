# Cloud Relay Backend Stub (API Contract)

## Purpose
This document defines the minimal backend API needed to make the desktop app reachable from any device globally.

## Endpoints

### 1. POST /relay/card-job
**Purpose:** Website sends card job to backend for delivery to desktop app
**Request:**
```json
{
  "sha256Hash": "ABC123...",
  "deviceId": "default-device",
  "timestamp": "2025-03-03T22:45:00.000Z"
}
```
**Response:**
- `200 OK`: Job queued successfully
- `4xx/5xx`: Error (website will show error to user)

### 2. WebSocket: wss://api.vista-d-net.world/ws/desktop?deviceId=<id>
**Purpose:** Desktop app connects to receive jobs
**Connection:** Desktop app connects with unique deviceId
**Messages from backend to desktop:**
```json
{
  "type": "card-job",
  "jobId": "uuid",
  "sha256Hash": "ABC123..."
}
```
**Messages from desktop to backend (ACK):**
```json
{
  "type": "card-job-ack",
  "jobId": "uuid"
}
```

## Data Flow
1. Website tries direct desktop call first
2. If fails, sends to cloud relay endpoint
3. Backend queues job for deviceId
4. Desktop app receives via WebSocket and renders QR
5. Desktop sends ACK to backend

## Minimal Implementation Options
- **Node.js + Express + ws** (quick prototype)
- **ASP.NET Core + SignalR** (matches C# stack)
- **Firebase Realtime Database** (managed, minimal code)
- **Supabase Realtime** (managed, minimal code)

## Security Notes
- Use HTTPS/WSS everywhere
- Validate deviceId registration (optional for MVP)
- Rate limit POST /relay/card-job
- Consider job TTL (e.g., 15 minutes)

## Next Steps
Choose backend stack and implement these endpoints. The website and desktop app are already wired to use them.

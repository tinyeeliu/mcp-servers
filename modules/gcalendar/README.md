## Google Calendar MCP Service

Lightweight MCP module that wraps key Google Calendar REST endpoints for calendars and events. Tools assume an OAuth bearer token is available from `AuthManager.getAuthInfo(...)` under the `authToken` key.

### Auth model
- Retrieves `authToken` via `AuthManager.getAuthInfo(sessionId)` and sends it as `Authorization: Bearer <token>`.
- Session id is read from the transport context key `session-id`. If absent, the call still attempts to fetch auth info with an empty string.

### Supported tools → Google endpoints
- `listCalendars` → `GET /users/me/calendarList`
- `getCalendar` → `GET /calendars/{calendarId}`
- `listEvents` → `GET /calendars/{calendarId}/events`
- `getEvent` → `GET /calendars/{calendarId}/events/{eventId}`
- `createEvent` → `POST /calendars/{calendarId}/events`
- `updateEvent` → `PATCH /calendars/{calendarId}/events/{eventId}`
- `deleteEvent` → `DELETE /calendars/{calendarId}/events/{eventId}`

### Inputs (per tool)
- `listCalendars`: `maxResults` (1-250), `pageToken`.
- `getCalendar`: `calendarId` (required).
- `listEvents`: `calendarId` (required); optional `timeMin`, `timeMax` (RFC3339), `maxResults`, `pageToken`, `singleEvents`, `orderBy` (`startTime|updated`), `query`.
- `getEvent`: `calendarId`, `eventId` (both required).
- `createEvent`: `calendarId`, `summary`, `startTime`, `endTime` (required); optional `description`, `location`, `timeZone`.
- `updateEvent`: `calendarId`, `eventId` (required); optional `summary`, `description`, `location`, `startTime`, `endTime`, `timeZone` (at least one must be provided).
- `deleteEvent`: `calendarId`, `eventId` (required).

### Behaviors
- Base URL: `https://www.googleapis.com/calendar/v3`.
- Request bodies are JSON; timestamps are passed through as provided (expect RFC3339).
- Errors: non-2xx responses surface as `Google API error <status>: <body>`. JSON parse failures are reported as errors.
- Pagination: `pageToken` and `maxResults` are passed through when provided.

### Specs and assets
- Tool schemas: `src/main/resources/io/mcp/spec/gcalendar/tool/*.json`
- Prompts: `src/main/resources/io/mcp/spec/gcalendar/prompt/*.json`
- Resources (docs links): `src/main/resources/io/mcp/spec/gcalendar/resource/*.json`
- URI templates: `src/main/resources/io/mcp/spec/gcalendar/template/*.json`

### Example calls
- List calendars: `{ "name": "listCalendars", "arguments": { "maxResults": 50 } }`
- List events in window: `{ "name": "listEvents", "arguments": { "calendarId": "primary", "timeMin": "2025-01-01T00:00:00Z", "timeMax": "2025-01-31T23:59:59Z", "singleEvents": true } }`
- Create event: `{ "name": "createEvent", "arguments": { "calendarId": "primary", "summary": "Demo", "startTime": "2025-02-01T10:00:00Z", "endTime": "2025-02-01T11:00:00Z", "timeZone": "UTC" } }`
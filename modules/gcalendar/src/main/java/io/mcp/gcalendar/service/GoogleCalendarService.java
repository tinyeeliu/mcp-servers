package io.mcp.gcalendar.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mcp.core.base.BaseMcpService;
import io.mcp.core.manager.AuthManager;
import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.Utility;
import io.mcp.gcalendar.tool.CreateEvent;
import io.mcp.gcalendar.tool.DeleteEvent;
import io.mcp.gcalendar.tool.GetCalendar;
import io.mcp.gcalendar.tool.GetEvent;
import io.mcp.gcalendar.tool.ListCalendars;
import io.mcp.gcalendar.tool.ListEvents;
import io.mcp.gcalendar.tool.UpdateEvent;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

public class GoogleCalendarService extends BaseMcpService {

    private static final String BASE_URL = "https://www.googleapis.com/calendar/v3";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;


    public GoogleCalendarService() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
        Utility.debug("GoogleCalendarService constructor");
    }

    public GoogleCalendarService(HttpClient httpClient, ObjectMapper objectMapper) {

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Implementation getServerInfo() {
        return new Implementation("mcp-gcalendar-server", "1.0.0");
    }

    @Override
    public List<McpTool> getTools() {
        return List.of(
            new ListCalendars(this),
            new GetCalendar(this),
            new ListEvents(this),
            new GetEvent(this),
            new CreateEvent(this),
            new UpdateEvent(this),
            new DeleteEvent(this)
        );
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }



    private URI buildUri(String path, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(BASE_URL).append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                if (!first) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return URI.create(sb.toString());
    }

    private HttpRequest.Builder requestBuilder(String token, URI uri) {
        return HttpRequest.newBuilder(uri)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json");
    }

    private CompletableFuture<JsonNode> send(HttpRequest request) {
        return send(request, null, null);
    }

    private CompletableFuture<JsonNode> send(HttpRequest request, String method, String requestBody) {
        // Log request details
        Utility.debug("HTTP Request - URL: " + request.uri() +
                     ", Method: " + (method != null ? method : "UNKNOWN"));
        if (requestBody != null && !requestBody.isEmpty()) {
            Utility.debug("HTTP Request Body: " + requestBody);
        }

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                String body = response.body();
                int status = response.statusCode();

                // Log response details
                Utility.debug("HTTP Response - Status: " + status + ", Body: " + body);

                if (status >= 200 && status < 300) {
                    try {
                        if (body == null || body.isBlank()) {
                            ObjectNode node = objectMapper.createObjectNode();
                            node.put("status", status);
                            return node;
                        }
                        return objectMapper.readTree(body);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse Google response: " + e.getMessage(), e);
                    }
                }
                throw new RuntimeException("Google API error " + status + ": " + body);
            });
    }

    public CompletableFuture<JsonNode> listCalendars(String token, Integer maxResults, String pageToken) {

        Utility.debug("listCalendars", token, maxResults, pageToken);

        Map<String, String> query = new HashMap<>();
        if (maxResults != null) {
            query.put("maxResults", maxResults.toString());
        }
        if (pageToken != null) {
            query.put("pageToken", pageToken);
        }
        URI uri = buildUri("/users/me/calendarList", query);
        HttpRequest request = requestBuilder(token, uri).GET().build();
        return send(request, "GET", null);
    }

    public CompletableFuture<JsonNode> getCalendar(String token, String calendarId) {
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId), Map.of());
        HttpRequest request = requestBuilder(token, uri).GET().build();
        return send(request, "GET", null);
    }

    public CompletableFuture<JsonNode> listEvents(
        String token,
        String calendarId,
        String timeMin,
        String timeMax,
        Integer maxResults,
        String pageToken,
        Boolean singleEvents,
        String orderBy,
        String queryText
    ) {
        Map<String, String> query = new HashMap<>();
        if (timeMin != null) {
            query.put("timeMin", timeMin);
        }
        if (timeMax != null) {
            query.put("timeMax", timeMax);
        }
        if (maxResults != null) {
            query.put("maxResults", maxResults.toString());
        }
        if (pageToken != null) {
            query.put("pageToken", pageToken);
        }
        if (singleEvents != null) {
            query.put("singleEvents", singleEvents.toString());
        }
        if (orderBy != null) {
            query.put("orderBy", orderBy);
        }
        if (queryText != null) {
            query.put("q", queryText);
        }
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId) + "/events", query);
        HttpRequest request = requestBuilder(token, uri).GET().build();
        return send(request, "GET", null);
    }

    public CompletableFuture<JsonNode> getEvent(String token, String calendarId, String eventId) {
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId) + "/events/" + encodeSegment(eventId), Map.of());
        HttpRequest request = requestBuilder(token, uri).GET().build();
        return send(request, "GET", null);
    }

    public CompletableFuture<JsonNode> createEvent(
        String token,
        String calendarId,
        String summary,
        String description,
        String location,
        String startTime,
        String endTime,
        String timeZone
    ) {
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId) + "/events", Map.of());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("summary", summary);
        if (description != null) {
            payload.put("description", description);
        }
        if (location != null) {
            payload.put("location", location);
        }
        ObjectNode startNode = payload.putObject("start");
        startNode.put("dateTime", startTime);
        if (timeZone != null) {
            startNode.put("timeZone", timeZone);
        }
        ObjectNode endNode = payload.putObject("end");
        endNode.put("dateTime", endTime);
        if (timeZone != null) {
            endNode.put("timeZone", timeZone);
        }
        String payloadStr = payload.toString();
        HttpRequest request = requestBuilder(token, uri)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
            .build();
        return send(request, "POST", payloadStr);
    }

    public CompletableFuture<JsonNode> updateEvent(
        String token,
        String calendarId,
        String eventId,
        String summary,
        String description,
        String location,
        String startTime,
        String endTime,
        String timeZone
    ) {
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId) + "/events/" + encodeSegment(eventId), Map.of());
        ObjectNode payload = objectMapper.createObjectNode();
        if (summary != null) {
            payload.put("summary", summary);
        }
        if (description != null) {
            payload.put("description", description);
        }
        if (location != null) {
            payload.put("location", location);
        }
        if (startTime != null) {
            ObjectNode startNode = payload.putObject("start");
            startNode.put("dateTime", startTime);
            if (timeZone != null) {
                startNode.put("timeZone", timeZone);
            }
        }
        if (endTime != null) {
            ObjectNode endNode = payload.putObject("end");
            endNode.put("dateTime", endTime);
            if (timeZone != null) {
                endNode.put("timeZone", timeZone);
            }
        }
        String payloadStr = payload.toString();
        HttpRequest request = requestBuilder(token, uri)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(payloadStr))
            .build();
        return send(request, "PATCH", payloadStr);
    }

    public CompletableFuture<JsonNode> deleteEvent(String token, String calendarId, String eventId) {
        URI uri = buildUri("/calendars/" + encodeSegment(calendarId) + "/events/" + encodeSegment(eventId), Map.of());
        HttpRequest request = requestBuilder(token, uri).DELETE().build();
        return send(request, "DELETE", null);
    }

    /*
    public String extractSessionId(Object transportContext) {
        if (transportContext == null) {
            return null;
        }

        // Direct map access if provided
        if (transportContext instanceof Map<?, ?> map) {
            Object sessionId = map.get("session-id");
            return sessionId != null ? sessionId.toString() : null;
        }

        // Fallback: attempt reflective get(String) for McpTransportContext or similar
        try {
            var method = transportContext.getClass().getMethod("get", Object.class);
            Object sessionId = method.invoke(transportContext, "session-id");
            return sessionId != null ? sessionId.toString() : null;
        } catch (Exception ignored) {
            // ignore and fall through
        }

        return null;
    }*/

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

package io.mcp.gcalendar.tool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.base.BaseMcpTool;
import io.mcp.gcalendar.service.GoogleCalendarService;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class UpdateEvent extends BaseMcpTool {

    private final GoogleCalendarService service;

    public UpdateEvent(GoogleCalendarService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "updateEvent";
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }

    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments();
            String calendarId = require(args, "calendarId");
            String eventId = require(args, "eventId");
            String summary = optional(args, "summary");
            String description = optional(args, "description");
            String location = optional(args, "location");
            String startTime = optional(args, "startTime");
            String endTime = optional(args, "endTime");
            String timeZone = optional(args, "timeZone");
            if (summary == null && description == null && location == null && startTime == null && endTime == null && timeZone == null) {
                throw new IllegalArgumentException("At least one field to update must be provided");
            }
            String sessionId = service.extractSessionId(exchange.transportContext());
            return service.fetchAuthToken(sessionId)
                .thenCompose(token -> service.updateEvent(token, calendarId, eventId, summary, description, location, startTime, endTime, timeZone))
                .thenApply(this::success)
                .exceptionally(this::failure);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(failure(e));
        }
    }

    private String require(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException(key + " is required");
        }
        Object value = args.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }

    private String optional(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            return null;
        }
        Object value = args.get(key);
        return value != null && !value.toString().isBlank() ? value.toString() : null;
    }

    private CallToolResult success(JsonNode node) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(node.toString())
            .isError(false)
            .build();
    }

    private CallToolResult failure(Throwable error) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(error.getMessage())
            .isError(true)
            .build();
    }
}

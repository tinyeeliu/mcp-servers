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

public class DeleteEvent extends BaseMcpTool {

    private final GoogleCalendarService service;

    public DeleteEvent(GoogleCalendarService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "deleteEvent";
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
            String sessionId = exchange.sessionId();
            return service.fetchAuthToken(sessionId)
                .thenCompose(token -> service.deleteEvent(token, calendarId, eventId))
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

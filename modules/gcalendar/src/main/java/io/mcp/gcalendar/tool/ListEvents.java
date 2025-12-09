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

public class ListEvents extends BaseMcpTool {

    private final GoogleCalendarService service;

    public ListEvents(GoogleCalendarService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "listEvents";
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }

    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments();
            String calendarId = parseRequiredString(args, "calendarId");
            String timeMin = parseString(args, "timeMin");
            String timeMax = parseString(args, "timeMax");
            Integer maxResults = parseInteger(args, "maxResults");
            String pageToken = parseString(args, "pageToken");
            Boolean singleEvents = parseBoolean(args, "singleEvents");
            String orderBy = parseString(args, "orderBy");
            String query = parseString(args, "query");
            String sessionId = service.extractSessionId(exchange.transportContext());
            return service.fetchAuthToken(sessionId)
                .thenCompose(token -> service.listEvents(token, calendarId, timeMin, timeMax, maxResults, pageToken, singleEvents, orderBy, query))
                .thenApply(this::success)
                .exceptionally(this::failure);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(failure(e));
        }
    }

    private String parseRequiredString(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException(key + " is required");
        }
        Object value = args.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }

    private String parseString(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer parseInteger(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key) || args.get(key) == null) {
            return null;
        }
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException(key + " must be a number");
    }

    private Boolean parseBoolean(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            return null;
        }
        Object value = args.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        throw new IllegalArgumentException(key + " must be a boolean");
    }

    private CallToolResult success(JsonNode node) {
        return McpSchema.CallToolResult.builder()
            .addTextContent(node.toPrettyString())
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

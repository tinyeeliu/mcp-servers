package io.mcp.gcalendar.tool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.base.BaseMcpTool;
import io.mcp.core.utility.Utility;
import io.mcp.gcalendar.service.GoogleCalendarService;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public class ListCalendars extends BaseMcpTool {

    private final GoogleCalendarService service;

    public ListCalendars(GoogleCalendarService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "listCalendars";
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }

    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request) {

        Utility.debug("ListCalendars tool call" );
        try {
            Map<String, Object> args = request.arguments();
            Integer maxResults = parseInteger(args, "maxResults");
            String pageToken = parseString(args, "pageToken");
            String sessionId = exchange.sessionId();
            return service.fetchAuthToken(sessionId)
                .thenCompose(token -> service.listCalendars(token, maxResults, pageToken))
                .thenApply(this::success)
                .exceptionally(this::failure);
        } catch (Exception e) {
            Utility.debug(e);
            return CompletableFuture.completedFuture(failure(e));
        }
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

    private String parseString(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        return value != null ? value.toString() : null;
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

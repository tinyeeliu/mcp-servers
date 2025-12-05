package io.mcp.core.server;

import static io.mcp.core.utility.Utility.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * Streamable HTTP server implementation for MCP.
 * 
 * This class provides HTTP request/response handling for MCP protocol
 * WITHOUT embedding an HTTP server. It's designed to be integrated with
 * any HTTP framework (Servlet containers, serverless functions, etc.).
 * 
 * Usage:
 * <pre>
 * StreamableServer server = new StreamableServer();
 * server.initialize(mcpService);
 * 
 * // In your HTTP handler (Servlet, Lambda, etc.):
 * String response = server.handleRequest(requestBody, sessionId).join();
 * </pre>
 */
public class StreamableServer {
    
    private final ObjectMapper objectMapper;
    private McpService mcpService;
    private Map<String, McpServerFeatures.AsyncToolSpecification> toolMap;
    
    // Session management for stateful connections
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    
    public StreamableServer() {
        this.objectMapper = new ObjectMapper();
    }
    
    public StreamableServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize the server with an MCP service.
     * Must be called before handling requests.
     */
    public void initialize(McpService mcpService) {
        debug("StreamableServer.initialize() - Starting initialization");
        this.mcpService = mcpService;
        this.toolMap = new HashMap<>();
        
        for (McpTool tool : mcpService.getTools()) {
            var spec = tool.getToolSpecification();
            toolMap.put(spec.tool().name(), spec);
            debug("  Registered tool:", spec.tool().name());
        }
        debug("StreamableServer.initialize() - Completed with", toolMap.size(), "tools");
    }

    /**
     * Handle an HTTP POST request containing a JSON-RPC message.
     * 
     * @param requestBody The raw JSON-RPC request body
     * @param sessionId Optional session ID for stateful connections (can be null)
     * @return CompletableFuture with the JSON-RPC response as a string
     */
    public CompletableFuture<String> handleRequest(String requestBody, String sessionId) {
        debug(">>> handleRequest - sessionId:", sessionId);
        debug(">>> Request body:", requestBody);
        try {
            JsonNode request = objectMapper.readTree(requestBody);
            return processJsonRpcRequest(request, sessionId)
                    .thenApply(response -> {
                        // Notifications return null - no response should be sent
                        if (response == null) {
                            debug("<<< Response: (none - notification)");
                            return null;
                        }
                        try {
                            String responseStr = objectMapper.writeValueAsString(response);
                            debug("<<< Response:", responseStr);
                            return responseStr;
                        } catch (Exception e) {
                            debug("!!! Serialization error:", e.getMessage());
                            return createErrorResponse(null, -32603, "Serialization error: " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            debug("!!! Parse error:", e.getMessage());
            return CompletableFuture.completedFuture(
                    createErrorResponse(null, -32700, "Parse error: " + e.getMessage()));
        }
    }
    
    /**
     * Handle an HTTP request synchronously.
     * 
     * @param requestBody The raw JSON-RPC request body
     * @param sessionId Optional session ID for stateful connections (can be null)
     * @return The JSON-RPC response as a string
     */
    public String handleRequestSync(String requestBody, String sessionId) {
        return handleRequest(requestBody, sessionId).join();
    }
    
    /**
     * Handle an HTTP request using Reader/Writer for servlet integration.
     * 
     * @param reader BufferedReader from HttpServletRequest
     * @param writer PrintWriter from HttpServletResponse
     * @param sessionId Optional session ID
     */
    public void handleRequest(BufferedReader reader, PrintWriter writer, String sessionId) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        
        String response = handleRequest(body.toString(), sessionId).join();
        writer.write(response);
        writer.flush();
    }

    /**
     * Create a Server-Sent Events (SSE) stream for the given session.
     * Used for streaming responses in Streamable HTTP transport.
     * 
     * @param sessionId The session ID
     * @param eventConsumer Consumer that receives SSE event strings
     * @return Runnable to close the stream
     */
    public Runnable createSseStream(String sessionId, Consumer<String> eventConsumer) {
        SessionState session = sessions.computeIfAbsent(sessionId, k -> new SessionState());
        session.sseConsumer = eventConsumer;
        
        return () -> {
            SessionState s = sessions.get(sessionId);
            if (s != null) {
                s.sseConsumer = null;
            }
        };
    }

    /**
     * Close a session and clean up resources.
     */
    public void closeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Shutdown the server and release all resources.
     */
    public void shutdown() {
        sessions.clear();
    }

    /**
     * Get the Content-Type header that should be used for responses.
     */
    public String getContentType() {
        return "application/json";
    }

    private CompletableFuture<JsonNode> processJsonRpcRequest(JsonNode request, String sessionId) {
        try {
            String method = request.path("method").asText();
            JsonNode params = request.path("params");
            JsonNode id = request.path("id");
            
            debug("--- Processing JSON-RPC method:", method, "id:", id);
            
            // Route to appropriate handler based on method
            return switch (method) {
                case "initialize" -> {
                    debug("    Handling initialize request");
                    yield CompletableFuture.completedFuture(handleInitialize(params, id));
                }
                case "initialized" -> {
                    debug("    Handling initialized notification");
                    yield CompletableFuture.completedFuture(handleInitialized(id));
                }
                case "tools/list" -> {
                    debug("    Handling tools/list request");
                    yield CompletableFuture.completedFuture(handleToolsList(id));
                }
                case "tools/call" -> {
                    debug("    Handling tools/call request - params:", params);
                    yield handleToolsCall(params, id);
                }
                case "ping" -> {
                    debug("    Handling ping request");
                    yield CompletableFuture.completedFuture(handlePing(id));
                }
                case "notifications/cancelled", "notifications/initialized" -> {
                    debug("    Handling notification:", method);
                    yield CompletableFuture.completedFuture(handleNotification());
                }
                default -> {
                    debug("!!! Unknown method:", method);
                    yield CompletableFuture.completedFuture(
                            createErrorResponseNode(id, -32601, "Method not found: " + method));
                }
            };
        } catch (Exception e) {
            debug("!!! Internal error in processJsonRpcRequest:", e.getMessage());
            e.printStackTrace(System.err);
            return CompletableFuture.completedFuture(
                    createErrorResponseNode(request.path("id"), -32603, "Internal error: " + e.getMessage()));
        }
    }

    private JsonNode handleInitialize(JsonNode params, JsonNode id) {
        debug("    Initialize params:", params);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        
        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", mcpService.getServerInfo().name());
        serverInfo.put("version", mcpService.getServerInfo().version());
        result.set("serverInfo", serverInfo);
        
        debug("    Server info - name:", mcpService.getServerInfo().name(), 
              "version:", mcpService.getServerInfo().version());
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode tools = objectMapper.createObjectNode();
        capabilities.set("tools", tools);
        result.set("capabilities", capabilities);
        
        debug("    Initialize response prepared");
        return createSuccessResponseNode(id, result);
    }

    private JsonNode handleInitialized(JsonNode id) {
        // This is a notification, typically no response needed
        // But we return success for compatibility
        return createSuccessResponseNode(id, objectMapper.createObjectNode());
    }

    private JsonNode handleToolsList(JsonNode id) {
        debug("    Listing", mcpService.getTools().size(), "tools");
        
        ObjectNode result = objectMapper.createObjectNode();
        var toolsArray = objectMapper.createArrayNode();
        
        for (McpTool tool : mcpService.getTools()) {
            var spec = tool.getToolSpecification();
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("name", spec.tool().name());
            toolNode.put("description", spec.tool().description());
            
            debug("      Tool:", spec.tool().name(), "-", spec.tool().description());
            
            // Convert inputSchema object to JSON node directly (not via toString())
            JsonNode inputSchema = objectMapper.valueToTree(spec.tool().inputSchema());
            toolNode.set("inputSchema", inputSchema);
            
            toolsArray.add(toolNode);
        }
        
        result.set("tools", toolsArray);
        debug("    tools/list response prepared");
        return createSuccessResponseNode(id, result);
    }

    private CompletableFuture<JsonNode> handleToolsCall(JsonNode params, JsonNode id) {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        
        debug("    Tool call - name:", toolName);
        debug("    Tool call - arguments:", arguments);
        
        McpServerFeatures.AsyncToolSpecification spec = toolMap.get(toolName);
        if (spec == null) {
            debug("!!! Unknown tool:", toolName, "- Available tools:", toolMap.keySet());
            return CompletableFuture.completedFuture(
                    createErrorResponseNode(id, -32602, "Unknown tool: " + toolName));
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.convertValue(arguments, Map.class);
            debug("    Converted arguments:", args);
            
            // Create CallToolRequest for the tool handler
            CallToolRequest request = new CallToolRequest(toolName, args);
            debug("    Invoking tool handler for:", toolName);
            
            return spec.callHandler().apply(null, request)
                    .toFuture()
                    .thenApply(result -> {
                        debug("    Tool", toolName, "completed successfully");
                        debug("    Tool result content count:", result.content().size());
                        return createToolResultResponse(id, result);
                    })
                    .exceptionally(e -> {
                        debug("!!! Tool execution error for", toolName, ":", e.getMessage());
                        e.printStackTrace(System.err);
                        return createErrorResponseNode(id, -32603, 
                                "Tool execution error: " + e.getMessage());
                    });
        } catch (Exception e) {
            debug("!!! Exception during tool call setup:", e.getMessage());
            e.printStackTrace(System.err);
            return CompletableFuture.completedFuture(
                    createErrorResponseNode(id, -32603, "Tool execution error: " + e.getMessage()));
        }
    }

    private JsonNode createToolResultResponse(JsonNode id, McpSchema.CallToolResult result) {
        ObjectNode resultNode = objectMapper.createObjectNode();
        var contentArray = objectMapper.createArrayNode();
        
        for (var content : result.content()) {
            ObjectNode contentNode = objectMapper.createObjectNode();
            if (content instanceof McpSchema.TextContent textContent) {
                contentNode.put("type", "text");
                contentNode.put("text", textContent.text());
            } else if (content instanceof McpSchema.ImageContent imageContent) {
                contentNode.put("type", "image");
                contentNode.put("data", imageContent.data());
                contentNode.put("mimeType", imageContent.mimeType());
            }
            contentArray.add(contentNode);
        }
        
        resultNode.set("content", contentArray);
        if (result.isError() != null) {
            resultNode.put("isError", result.isError());
        }
        
        return createSuccessResponseNode(id, resultNode);
    }

    private JsonNode handlePing(JsonNode id) {
        return createSuccessResponseNode(id, objectMapper.createObjectNode());
    }

    private JsonNode handleNotification() {
        // Notifications don't require a response
        return null;
    }

    private JsonNode createSuccessResponseNode(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private JsonNode createErrorResponseNode(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        
        return response;
    }

    private String createErrorResponse(String id, int code, String message) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            if (id != null) {
                response.put("id", id);
            } else {
                response.putNull("id");
            }
            
            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", code);
            error.put("message", message);
            response.set("error", error);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }

    /**
     * Session state holder for stateful connections.
     */
    private static class SessionState {
        @SuppressWarnings("unused")
        Consumer<String> sseConsumer;
        // Add more session state as needed
    }


}

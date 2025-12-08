package io.mcp.core.server;

import static io.mcp.core.utility.Utility.debug;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.mcp.core.command.StatusCommand;
import io.mcp.core.protocol.McpService;
import io.mcp.core.utility.ServiceUtility;
import io.mcp.core.utility.Utility;

/**
 * Pure Java implementation of MCP HTTP server.
 *
 * Supports both SSE and Streamable HTTP transport with module-specific endpoints:
 *
 * SSE Transport (legacy):
 * - GET /{module}/sse - Establishes SSE connection, returns endpoint URL
 * - POST /{module}/messages?sessionId=xxx - Sends messages to server
 *
 * Streamable HTTP Transport (modern):
 * - POST /{module}/mcp - Handles JSON-RPC requests, can return JSON or SSE stream
 */
public class McpHttpServer {


    private final Map<String, StreamableServer> moduleServers = new ConcurrentHashMap<>();
    private HttpServer httpServer;
    private final int port;
    
    // Session management for SSE connections
    private final Map<String, SseSession> sseSessions = new ConcurrentHashMap<>();

    public McpHttpServer() {
        this(Utility.getConfiguredPort());
    }


    public McpHttpServer(int port) {
        this.port = port;
        initializeModuleServers();
    }

    private void initializeModuleServers() {
        List<McpService> services = ServiceUtility.getRegisteredServices();
        debug("Initializing module servers for", services.size(), "services");

        for (McpService service : services) {
            String moduleName = service.getModule();
            StreamableServer server = new StreamableServer();
            server.initialize(service);
            moduleServers.put(moduleName, server);
            debug("Initialized server for module:", moduleName);
        }
    }

    /**
     * Start the server with SSE transport (legacy mode).
     *
     * SSE transport uses module-specific endpoints:
     * - GET /{module}/sse - Client connects here to establish SSE stream
     * - POST /{module}/messages?sessionId=xxx - Client sends requests here
     */
    public void startSseServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Register status endpoint
        httpServer.createContext("/mcp/status.json", this::handleStatusRequest);

        // Register module-specific SSE endpoints
        for (String moduleName : moduleServers.keySet()) {
            String ssePath = "/" + moduleName + "/sse";
            String messagePath = "/" + moduleName + "/messages";

            httpServer.createContext(ssePath, exchange -> handleModuleSseConnection(exchange, moduleName));
            httpServer.createContext(messagePath, exchange -> handleModuleSseMessage(exchange, moduleName));
        }

        // Use virtual threads for concurrent SSE connections
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();

        System.out.println("MCP SSE Server running on http://localhost:" + port);
        for (String moduleName : moduleServers.keySet()) {
            System.out.println("  Module '" + moduleName + "' SSE endpoint: http://localhost:" + port + "/" + moduleName + "/sse");
            System.out.println("  Module '" + moduleName + "' Message endpoint: http://localhost:" + port + "/" + moduleName + "/messages");
        }
    }

    /**
     * Start the server with Streamable HTTP transport (modern mode).
     *
     * Streamable HTTP uses module-specific endpoints:
     * - POST /{module}/mcp - Handles all JSON-RPC requests for each module
     *
     * Response can be either:
     * - application/json for simple responses
     * - text/event-stream for streaming responses
     */
    public void startStreamableServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Register status endpoint
        httpServer.createContext("/mcp/status.json", this::handleStatusRequest);

        // Register module-specific endpoints for streamable HTTP
        for (String moduleName : moduleServers.keySet()) {
            String path = "/" + moduleName + "/mcp";
            httpServer.createContext(path, exchange -> handleModuleStreamableRequest(exchange, moduleName));
        }

        // Use virtual threads for concurrent connections
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();

        System.out.println("MCP Streamable HTTP Server running on http://localhost:" + port);
        for (String moduleName : moduleServers.keySet()) {
            System.out.println("  Module '" + moduleName + "' endpoint: http://localhost:" + port + "/" + moduleName + "/mcp");
        }
    }

    /**
     * Start the server with both SSE and Streamable HTTP support.
     */
    public void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Register status endpoint
        httpServer.createContext("/mcp/status.json", this::handleStatusRequest);

        // Register module-specific endpoints for both transport types
        for (String moduleName : moduleServers.keySet()) {
            String mcpPath = "/" + moduleName + "/mcp";
            String ssePath = "/" + moduleName + "/sse";
            String messagePath = "/" + moduleName + "/messages";

            httpServer.createContext(mcpPath, exchange -> handleModuleStreamableRequest(exchange, moduleName));
            httpServer.createContext(ssePath, exchange -> handleModuleSseConnection(exchange, moduleName));
            httpServer.createContext(messagePath, exchange -> handleModuleSseMessage(exchange, moduleName));
        }

        // Use virtual threads for concurrent connections
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();

        System.out.println("MCP HTTP Server running on http://localhost:" + port);
        for (String moduleName : moduleServers.keySet()) {
            System.out.println("  Module '" + moduleName + "' Streamable endpoint: http://localhost:" + port + "/" + moduleName + "/mcp");
            System.out.println("  Module '" + moduleName + "' SSE endpoint: http://localhost:" + port + "/" + moduleName + "/sse");
        }
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (httpServer != null) {
            // Close all SSE sessions
            for (SseSession session : sseSessions.values()) {
                session.close();
            }
            sseSessions.clear();
            
            httpServer.stop(0);
            httpServer = null;
            debug("MCP HTTP Server stopped");
        }
    }

    /**
     * Handle module-specific Streamable HTTP POST requests.
     */
    private void handleModuleStreamableRequest(HttpExchange exchange, String moduleName) throws IOException {
        StreamableServer server = moduleServers.get(moduleName);
        if (server == null) {
            sendError(exchange, 404, "Module not found: " + moduleName);
            return;
        }

        handleStreamableRequest(exchange, server);
    }

    /**
     * Handle Streamable HTTP POST requests with specified server.
     */
    private void handleStreamableRequest(HttpExchange exchange, StreamableServer server) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            debug("Streamable request:", requestBody);

            // Get or create session ID from header
            String sessionId = exchange.getRequestHeaders().getFirst("Mcp-Session-Id");
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }

            // Check Accept header for SSE preference
            String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
            boolean preferSse = acceptHeader != null && acceptHeader.contains("text/event-stream");

            if (preferSse) {
                // Return response as SSE stream
                handleStreamableWithSse(exchange, requestBody, sessionId, server);
            } else {
                // Return response as JSON
                String response = server.handleRequestSync(requestBody, sessionId);

                // Notifications return null - send 202 Accepted with no body
                if (response == null) {
                    exchange.getResponseHeaders().set("Mcp-Session-Id", sessionId);
                    exchange.sendResponseHeaders(202, -1);
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", server.getContentType());
                exchange.getResponseHeaders().set("Mcp-Session-Id", sessionId);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        } catch (Exception e) {
            debug("Error handling streamable request:", e.getMessage());
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Handle Streamable HTTP request with SSE response.
     */
    private void handleStreamableWithSse(HttpExchange exchange, String requestBody, String sessionId, StreamableServer server) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Mcp-Session-Id", sessionId);
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream os = exchange.getResponseBody()) {
            // Process request and send response as SSE event
            String response = server.handleRequestSync(requestBody, sessionId);

            // Notifications return null - no SSE event to send
            if (response != null) {
                String sseEvent = "event: message\ndata: " + response.replace("\n", "\ndata: ") + "\n\n";
                os.write(sseEvent.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }
    }

    /**
     * Handle module-specific SSE connection establishment (GET /{module}/sse).
     */
    private void handleModuleSseConnection(HttpExchange exchange, String moduleName) throws IOException {
        StreamableServer server = moduleServers.get(moduleName);
        if (server == null) {
            sendError(exchange, 404, "Module not found: " + moduleName);
            return;
        }

        handleSseConnection(exchange, server, moduleName);
    }

    /**
     * Handle SSE connection establishment with specified server and module.
     */
    private void handleSseConnection(HttpExchange exchange, StreamableServer server, String moduleName) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        debug("New SSE connection, sessionId:", sessionId);

        // Set SSE headers
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        
        // Create session
        SseSession session = new SseSession(sessionId, os);
        sseSessions.put(sessionId, session);

        try {
            // Send endpoint event with message URL
            String messageUrl = "http://localhost:" + port + "/" + moduleName + "/messages?sessionId=" + sessionId;
            String endpointEvent = "event: endpoint\ndata: " + messageUrl + "\n\n";
            os.write(endpointEvent.getBytes(StandardCharsets.UTF_8));
            os.flush();
            debug("Sent endpoint event for module", moduleName + ":", messageUrl);

            // Keep connection alive until closed
            // The connection will be closed when the client disconnects
            // or when we call session.close()
            while (session.isOpen()) {
                try {
                    Thread.sleep(1000);
                    // Send keepalive
                    os.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (Exception e) {
                    debug("SSE connection closed:", sessionId);
                    break;
                }
            }
        } finally {
            sseSessions.remove(sessionId);
            try {
                os.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handle module-specific SSE message requests (POST /{module}/messages).
     */
    private void handleModuleSseMessage(HttpExchange exchange, String moduleName) throws IOException {
        StreamableServer server = moduleServers.get(moduleName);
        if (server == null) {
            sendError(exchange, 404, "Module not found: " + moduleName);
            return;
        }

        handleSseMessage(exchange, server);
    }

    /**
     * Handle SSE message requests with specified server.
     */
    private void handleSseMessage(HttpExchange exchange, StreamableServer server) throws IOException {
        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        // Extract session ID from query parameter
        String query = exchange.getRequestURI().getQuery();
        String sessionId = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "sessionId".equals(pair[0])) {
                    sessionId = pair[1];
                    break;
                }
            }
        }

        if (sessionId == null) {
            sendError(exchange, 400, "Missing sessionId parameter");
            return;
        }

        SseSession session = sseSessions.get(sessionId);
        if (session == null) {
            sendError(exchange, 404, "Session not found: " + sessionId);
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            debug("SSE message request:", requestBody);

            // Process the request
            String response = server.handleRequestSync(requestBody, sessionId);

            // Send response via SSE stream (null means notification - no response needed)
            if (response != null) {
                session.sendMessage(response);
            }

            // Send accepted response to POST request
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String accepted = "{\"status\":\"accepted\"}";
            byte[] acceptedBytes = accepted.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, acceptedBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(acceptedBytes);
            }
        } catch (Exception e) {
            debug("Error handling SSE message:", e.getMessage());
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Handle POST /mcp/status.json requests.
     */
    private void handleStatusRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // Execute the StatusCommand
            StatusCommand statusCommand = new StatusCommand();
            Map<String, Object> statusResult = statusCommand.execute().join();

            // Convert the Map to JSON using Jackson ObjectMapper
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(statusResult);

            // Send JSON response
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            debug("Error handling status request:", e.getMessage());
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Send an error response.
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = "{\"error\":\"" + message + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Get the port this server is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Check if the server is running.
     */
    public boolean isRunning() {
        return httpServer != null;
    }

    /**
     * SSE Session holder.
     */
    private static class SseSession {
        private final String sessionId;
        private final OutputStream outputStream;
        private volatile boolean open = true;

        SseSession(String sessionId, OutputStream outputStream) {
            this.sessionId = sessionId;
            this.outputStream = outputStream;
        }

        synchronized void sendMessage(String message) throws IOException {
            if (!open) return;
            String sseEvent = "event: message\ndata: " + message.replace("\n", "\ndata: ") + "\n\n";
            outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            debug("Sent SSE message to session:", sessionId);
        }

        boolean isOpen() {
            return open;
        }

        void close() {
            open = false;
            try {
                outputStream.close();
            } catch (Exception ignored) {}
        }
    }
}

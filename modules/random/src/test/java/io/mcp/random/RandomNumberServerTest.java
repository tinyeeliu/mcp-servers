package io.mcp.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.server.McpHttpServer;
import io.mcp.core.server.StreamableServer;
import io.mcp.random.service.RandomService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

class RandomNumberServerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test helper methods to reduce duplication

    /**
     * Creates a standard initialize request JSON
     */
    private String createInitializeRequest(int id) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {
                        "name": "test-client",
                        "version": "1.0.0"
                    }
                }
            }
            """, id);
    }

    /**
     * Creates an initialized notification JSON
     */
    private String createInitializedNotification() {
        return """
            {
                "jsonrpc": "2.0",
                "method": "initialized"
            }
            """;
    }

    /**
     * Creates a tools/list request JSON
     */
    private String createListToolsRequest(int id) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "tools/list",
                "params": {}
            }
            """, id);
    }

    /**
     * Creates a tools/call request JSON
     */
    private String createCallToolRequest(int id, String toolName, Map<String, Object> arguments) {
        try {
            return String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": %d,
                    "method": "tools/call",
                    "params": {
                        "name": "%s",
                        "arguments": %s
                    }
                }
                """, id, toolName, objectMapper.writeValueAsString(arguments));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tool arguments", e);
        }
    }

    /**
     * Creates a prompts/list request JSON
     */
    private String createListPromptsRequest(int id) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "prompts/list",
                "params": {}
            }
            """, id);
    }

    /**
     * Creates a prompts/get request JSON
     */
    private String createGetPromptRequest(int id, String promptName, Map<String, Object> arguments) {
        try {
            String argsJson = arguments != null && !arguments.isEmpty() ?
                ", \"arguments\": " + objectMapper.writeValueAsString(arguments) : "";
            return String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": %d,
                    "method": "prompts/get",
                    "params": {
                        "name": "%s"%s
                    }
                }
                """, id, promptName, argsJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize prompt arguments", e);
        }
    }

    /**
     * Creates a resources/list request JSON
     */
    private String createListResourcesRequest(int id) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "resources/list",
                "params": {}
            }
            """, id);
    }

    /**
     * Creates a resources/read request JSON
     */
    private String createReadResourceRequest(int id, String uri) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "resources/read",
                "params": {
                    "uri": "%s"
                }
            }
            """, id, uri);
    }

    /**
     * Creates a resources/templates/list request JSON
     */
    private String createListTemplatesRequest(int id) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "resources/templates/list",
                "params": {}
            }
            """, id);
    }

    /**
     * Creates a resources/templates/read request JSON
     */
    private String createReadTemplateRequest(int id, String uriTemplate) {
        return String.format("""
            {
                "jsonrpc": "2.0",
                "id": %d,
                "method": "resources/templates/read",
                "params": {
                    "uriTemplate": "%s"
                }
            }
            """, id, uriTemplate);
    }

    /**
     * Validates a standard JSON-RPC response
     */
    private void validateJsonRpcResponse(String responseJson, int expectedId) throws Exception {
        JsonNode response = objectMapper.readTree(responseJson);
        assertEquals("2.0", response.path("jsonrpc").asText(), "Should be JSON-RPC 2.0");
        assertEquals(expectedId, response.path("id").asInt(), "ID should match request");
        assertFalse(response.has("error"), "Response should not have error: " + responseJson);
        assertTrue(response.has("result"), "Response should have result");
    }

    /**
     * Creates and initializes a StreamableServer with RandomService
     */
    private StreamableServer createInitializedServer() throws Exception {
        StreamableServer server = new StreamableServer();
        RandomService service = new RandomService();
        server.initialize(service);
        return server;
    }

    /**
     * Comprehensive test for stdio transport covering all MCP endpoints.
     *
     * Tests initialize, tools/list, tools/call, prompts/list, prompts/get,
     * resources/list, resources/read, templates/list, templates/read
     */
    @Test
    void testStdioTransportComprehensive() throws Exception {
        // Prepare comprehensive JSON-RPC requests for stdio transport
        StringBuilder allRequests = new StringBuilder();

        // 1. Initialize request
        allRequests.append(createInitializeRequest(1).replace("\n", "").replace(" ", "")).append("\n");

        // 2. Initialized notification
        allRequests.append(createInitializedNotification().replace("\n", "").replace(" ", "")).append("\n");

        // 3. List tools
        allRequests.append(createListToolsRequest(2).replace("\n", "").replace(" ", "")).append("\n");

        // 4. Call generateRandom tool
        allRequests.append(createCallToolRequest(3, "generateRandom", Map.of("bound", 100)).replace("\n", "").replace(" ", "")).append("\n");

        // 5. List prompts
        allRequests.append(createListPromptsRequest(4).replace("\n", "").replace(" ", "")).append("\n");

        // 6. Get random_game prompt
        allRequests.append(createGetPromptRequest(5, "random_game", Map.of("bound", 6)).replace("\n", "").replace(" ", "")).append("\n");

        // 7. List resources
        allRequests.append(createListResourcesRequest(6).replace("\n", "").replace(" ", "")).append("\n");

        // 8. Read the wikipedia resource
        allRequests.append(createReadResourceRequest(7, "https://en.wikipedia.org/wiki/Random_number_generation").replace("\n", "").replace(" ", "")).append("\n");

        // 9. List templates
        allRequests.append(createListTemplatesRequest(8).replace("\n", "").replace(" ", "")).append("\n");

        // Note: resources/templates/read is not supported by the MCP SDK async server yet
        // So we skip testing templates/read for now

        // Save original stdin/stdout
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            // Redirect stdin with our test input
            System.setIn(new ByteArrayInputStream(allRequests.toString().getBytes()));
            // Redirect stdout to capture responses
            System.setOut(new PrintStream(capturedOutput));

            // Create and start the stdio server in a separate thread
            Thread serverThread = new Thread(() -> {
                try {
                    RandomService service = new RandomService();
                    var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
                    var transportProvider = new StdioServerTransportProvider(jsonMapper);

                    McpAsyncServer server = McpServer.async(transportProvider)
                            .serverInfo(service.getServerInfo())
                            .capabilities(McpSchema.ServerCapabilities.builder()
                                    .tools(true)
                                    .prompts(true)
                                    .resources(true, false)
                                    .build())
                            .tools(service.getTools().stream()
                                    .map(t -> t.getToolSpecification())
                                    .toList())
                            .prompts(service.getPromptSpecifications())
                            .resources(service.getResourceSpecifications())
                            .resourceTemplates(service.getResourceTemplateSpecifications())
                            .build();

                    // Give the server time to process all requests
                    Thread.sleep(3000);
                    server.close();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            });

            serverThread.start();

            // Wait for server to complete processing
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            // Restore streams before assertions
            System.setIn(originalIn);
            System.setOut(originalOut);

            String output = capturedOutput.toString();
            System.out.println("Captured stdio output: " + output);

            // Verify we got responses
            assertTrue(completed, "Server should complete processing");
            assertFalse(output.isEmpty(), "Server should produce output");

            // Parse and validate responses (stdio outputs one JSON-RPC message per line)
            String[] responseLines = output.trim().split("\\n");
            assertTrue(responseLines.length >= 8, "Should have at least 8 responses, got: " + responseLines.length);

            // Validate initialize response (id: 1)
            JsonNode initResponse = objectMapper.readTree(responseLines[0]);
            validateJsonRpcResponse(responseLines[0], 1);
            assertEquals("2024-11-05", initResponse.path("result").path("protocolVersion").asText());
            assertEquals("mcp-random-server", initResponse.path("result").path("serverInfo").path("name").asText());

            // Validate tools/list response (id: 2)
            JsonNode toolsResponse = objectMapper.readTree(responseLines[1]);
            validateJsonRpcResponse(responseLines[1], 2);
            JsonNode tools = toolsResponse.path("result").path("tools");
            assertTrue(tools.isArray() && tools.size() > 0, "Should have tools");
            boolean hasGenerateRandom = false;
            for (JsonNode tool : tools) {
                if ("generateRandom".equals(tool.path("name").asText())) {
                    hasGenerateRandom = true;
                    break;
                }
            }
            assertTrue(hasGenerateRandom, "generateRandom tool should be available");

            // Validate tools/call response (id: 3)
            JsonNode toolCallResponse = objectMapper.readTree(responseLines[2]);
            validateJsonRpcResponse(responseLines[2], 3);
            JsonNode toolResult = toolCallResponse.path("result");
            assertTrue(toolResult.has("content"), "Tool result should have content");
            JsonNode content = toolResult.path("content");
            assertTrue(content.isArray() && content.size() > 0, "Content should be array");
            assertEquals("text", content.get(0).path("type").asText());
            int randomNumber = Integer.parseInt(content.get(0).path("text").asText());
            assertTrue(randomNumber >= 0 && randomNumber < 100, "Random number should be valid");

            // Validate prompts/list response (id: 4)
            JsonNode promptsResponse = objectMapper.readTree(responseLines[3]);
            validateJsonRpcResponse(responseLines[3], 4);
            JsonNode prompts = promptsResponse.path("result").path("prompts");
            assertTrue(prompts.isArray() && prompts.size() >= 2, "Should have at least 2 prompts");

            // Validate prompts/get response (id: 5)
            JsonNode promptGetResponse = objectMapper.readTree(responseLines[4]);
            validateJsonRpcResponse(responseLines[4], 5);
            JsonNode promptResult = promptGetResponse.path("result");
            assertTrue(promptResult.has("description"), "Prompt should have description");
            assertTrue(promptResult.has("messages"), "Prompt should have messages");

            // Validate resources/list response (id: 6)
            JsonNode resourcesResponse = objectMapper.readTree(responseLines[5]);
            validateJsonRpcResponse(responseLines[5], 6);
            JsonNode resources = resourcesResponse.path("result").path("resources");
            assertTrue(resources.isArray() && resources.size() > 0, "Should have resources");

            // Validate resources/read response (id: 7)
            JsonNode resourceReadResponse = objectMapper.readTree(responseLines[6]);
            validateJsonRpcResponse(responseLines[6], 7);
            JsonNode resourceContents = resourceReadResponse.path("result").path("contents");
            assertTrue(resourceContents.isArray() && resourceContents.size() > 0, "Should have resource contents");

            // Validate templates/list response (id: 8)
            JsonNode templatesResponse = objectMapper.readTree(responseLines[7]);
            validateJsonRpcResponse(responseLines[7], 8);
            JsonNode templates = templatesResponse.path("result").path("resourceTemplates");
            assertTrue(templates.isArray() && templates.size() > 0, "Should have templates");

        } finally {
            // Ensure streams are restored
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    /**
     * Test 2: Open streamable-http transport and call the generateRandom tool.
     * 
     * This test uses the StreamableServer directly to test HTTP-based JSON-RPC
     * communication.
     */
    @Test
    void testStreamableHttpGenerateRandom() throws Exception {
        // Create and initialize the StreamableServer
        StreamableServer server = new StreamableServer();
        RandomService service = new RandomService();
        server.initialize(service);

        // Step 1: Send initialize request
        String initializeRequest = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {
                        "name": "test-client",
                        "version": "1.0.0"
                    }
                }
            }
            """;

        String initResponse = server.handleRequestSync(initializeRequest, null);
        JsonNode initJson = objectMapper.readTree(initResponse);

        // Verify initialize response
        assertEquals("2.0", initJson.path("jsonrpc").asText());
        assertEquals(1, initJson.path("id").asInt());
        assertFalse(initJson.has("error"), "Initialize should not return error");
        assertTrue(initJson.has("result"), "Initialize should return result");

        JsonNode result = initJson.path("result");
        assertEquals("2024-11-05", result.path("protocolVersion").asText());
        assertEquals("mcp-random-server", result.path("serverInfo").path("name").asText());

        // Step 2: Send initialized notification
        String initializedNotification = """
            {
                "jsonrpc": "2.0",
                "method": "initialized"
            }
            """;
        server.handleRequestSync(initializedNotification, null);

        // Step 3: List tools to verify generateRandom is available
        String listToolsRequest = """
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/list",
                "params": {}
            }
            """;

        String listToolsResponse = server.handleRequestSync(listToolsRequest, null);
        JsonNode listToolsJson = objectMapper.readTree(listToolsResponse);

        JsonNode tools = listToolsJson.path("result").path("tools");
        assertTrue(tools.isArray(), "Tools should be an array");
        assertTrue(tools.size() > 0, "Should have at least one tool");

        boolean hasGenerateRandom = false;
        for (JsonNode tool : tools) {
            if ("generateRandom".equals(tool.path("name").asText())) {
                hasGenerateRandom = true;
                break;
            }
        }
        assertTrue(hasGenerateRandom, "generateRandom tool should be available");

        // Step 4: Call generateRandom tool
        String callToolRequest = """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "generateRandom",
                    "arguments": {
                        "bound": 100
                    }
                }
            }
            """;

        String toolResponse = server.handleRequestSync(callToolRequest, null);
        JsonNode toolJson = objectMapper.readTree(toolResponse);

        // Verify tool call response
        assertEquals("2.0", toolJson.path("jsonrpc").asText());
        assertEquals(3, toolJson.path("id").asInt());
        assertFalse(toolJson.has("error"), "Tool call should not return error: " + toolResponse);
        assertTrue(toolJson.has("result"), "Tool call should return result");

        JsonNode toolResult = toolJson.path("result");
        assertTrue(toolResult.has("content"), "Result should have content");

        JsonNode content = toolResult.path("content");
        assertTrue(content.isArray(), "Content should be an array");
        assertTrue(content.size() > 0, "Content should have at least one item");

        JsonNode firstContent = content.get(0);
        assertEquals("text", firstContent.path("type").asText(), "Content type should be text");

        String textResult = firstContent.path("text").asText();
        int randomNumber = Integer.parseInt(textResult);
        assertTrue(randomNumber >= 0 && randomNumber < 100,
                "Random number should be between 0 and 100, got: " + randomNumber);

        // Cleanup
        server.shutdown();
    }

    /**
     * Additional test: Verify error handling when bound is invalid
     */
    @Test
    void testStreamableHttpGenerateRandomWithInvalidBound() throws Exception {
        StreamableServer server = new StreamableServer();
        RandomService service = new RandomService();
        server.initialize(service);

        // Call generateRandom with invalid bound (0)
        String callToolRequest = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "generateRandom",
                    "arguments": {
                        "bound": 0
                    }
                }
            }
            """;

        String toolResponse = server.handleRequestSync(callToolRequest, null);
        JsonNode toolJson = objectMapper.readTree(toolResponse);

        // The tool returns isError=true in the result when validation fails
        JsonNode result = toolJson.path("result");
        assertTrue(result.has("isError"), "Result should have isError field: " + toolResponse);
        assertTrue(result.path("isError").asBoolean(), "isError should be true for invalid bound");
        
        // Verify the error message is in the content
        JsonNode content = result.path("content");
        assertTrue(content.isArray() && content.size() > 0, "Content should have error message");
        String errorText = content.get(0).path("text").asText();
        assertTrue(errorText.contains("bound must be a positive integer"), 
                "Error message should mention invalid bound: " + errorText);

        server.shutdown();
    }


    /**
     * Test using the official MCP SDK's McpSyncClient with Streamable HTTP transport.
     * 
     * This test starts McpHttpServer, connects to it using
     * HttpClientStreamableHttpTransport, and calls the generateRandom tool.
     */
    @Test
    void testStreamableHttpTransportClientGenerateRandom() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = new StreamableServer();
        RandomService service = new RandomService();
        mcpServer.initialize(service);

        // Start McpHttpServer with Streamable HTTP transport
        int port = 18080;
        McpHttpServer httpServer = new McpHttpServer(mcpServer, port);
        httpServer.startStreamableServer();

        McpSyncClient client = null;
        try {
            // Create HTTP transport pointing to our server
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                    .builder("http://localhost:" + port)
                    .endpoint("/mcp")
                    .build();

            // Create MCP sync client
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(10))
                    .initializationTimeout(Duration.ofSeconds(10))
                    .build();

            // Initialize the client (performs handshake with server)
            client.initialize();

            // List available tools
            McpSchema.ListToolsResult toolsResult = client.listTools();
            assertNotNull(toolsResult, "Tools result should not be null");
            assertFalse(toolsResult.tools().isEmpty(), "Should have at least one tool");

            // Verify generateRandom tool is available
            boolean hasGenerateRandom = toolsResult.tools().stream()
                    .anyMatch(tool -> "generateRandom".equals(tool.name()));
            assertTrue(hasGenerateRandom, "generateRandom tool should be available");

            // Call the generateRandom tool
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest("generateRandom", Map.of("bound", 100)));

            // Verify the result
            assertNotNull(result, "Tool result should not be null");
            assertFalse(result.content().isEmpty(), "Result should have content");

            // Get the text content from the result
            var content = result.content().get(0);
            assertTrue(content instanceof McpSchema.TextContent, "Content should be TextContent");
            
            String textResult = ((McpSchema.TextContent) content).text();
            int randomNumber = Integer.parseInt(textResult);
            assertTrue(randomNumber >= 0 && randomNumber < 100,
                    "Random number should be between 0 and 100, got: " + randomNumber);

            System.out.println("Generated random number (Streamable HTTP): " + randomNumber);

        } finally {
            // Clean up resources
            if (client != null) {
                client.closeGracefully();
            }
            httpServer.stop();
            mcpServer.shutdown();
        }
    }

    /**
     * Test using McpHttpServer with SSE transport.
     * 
     * This test verifies the SSE server starts correctly and endpoints are available.
     */
    @Test
    void testSseServerStartup() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = new StreamableServer();
        RandomService service = new RandomService();
        mcpServer.initialize(service);

        // Start McpHttpServer with SSE transport
        int port = 18081;
        McpHttpServer httpServer = new McpHttpServer(mcpServer, port);
        httpServer.startSseServer();

        try {
            // Verify server is running
            assertTrue(httpServer.isRunning(), "SSE server should be running");
            assertEquals(port, httpServer.getPort(), "Server should be on correct port");

            // Test SSE endpoint responds (GET /sse should return event-stream)
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:" + port + "/sse"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            // Use async to avoid blocking on SSE stream (we just want to verify connection is accepted)
            httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            
            // Give server a moment to respond
            Thread.sleep(500);
            
            // The connection should be established (SSE keeps connection open)
            assertTrue(httpServer.isRunning(), "Server should still be running");

        } finally {
            httpServer.stop();
            mcpServer.shutdown();
        }
    }

    /**
     * Comprehensive test for SSE transport covering all MCP endpoints.
     *
     * This test connects to the SSE server and tests all endpoints by sending
     * JSON-RPC messages via POST and receiving responses via SSE stream.
     */
    @Test
    void testSseTransportComprehensive() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = createInitializedServer();

        // Start McpHttpServer with SSE transport
        int port = 18083;
        McpHttpServer httpServer = new McpHttpServer(mcpServer, port);
        httpServer.startSseServer();

        try {
            // Step 1: Establish SSE connection and get message endpoint
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest sseRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:" + port + "/sse"))
                    .GET()
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(5))
                    .build();

            // Start SSE connection in background thread to collect responses
            java.util.concurrent.CompletableFuture<String> sseFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    java.net.http.HttpResponse<java.io.InputStream> response = httpClient.send(sseRequest, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body()));
                    StringBuilder collectedData = new StringBuilder();

                    String line;
                    boolean gotEndpoint = false;
                    String messageUrl = null;

                    // Read SSE stream
                    while ((line = reader.readLine()) != null) {
                        collectedData.append(line).append("\n");

                        if (line.startsWith("data: http://localhost:" + port + "/messages?sessionId=")) {
                            messageUrl = line.substring(6).trim(); // Remove "data: " prefix
                            gotEndpoint = true;
                            break; // We got what we need for the test
                        }

                        // Break after reasonable time to avoid hanging
                        if (collectedData.length() > 10000) break;
                    }

                    // Return the message URL for the main test
                    return messageUrl;
                } catch (Exception e) {
                    throw new RuntimeException("SSE connection failed", e);
                }
            });

            // Wait for SSE connection to establish and get message URL
            String messageUrl = sseFuture.get(10, TimeUnit.SECONDS);
            assertNotNull(messageUrl, "Should receive message endpoint URL");
            assertTrue(messageUrl.contains("/messages?sessionId="), "Should contain message endpoint");

            // Extract sessionId from URL
            String sessionId = messageUrl.substring(messageUrl.indexOf("sessionId=") + 10);

            // Step 2: Test initialize via SSE messages
            String initResponse = sendSseMessage(httpClient, messageUrl, createInitializeRequest(1));
            validateJsonRpcResponse(initResponse, 1);
            JsonNode initJson = objectMapper.readTree(initResponse);
            assertEquals("2024-11-05", initJson.path("result").path("protocolVersion").asText());

            // Step 3: Send initialized notification
            sendSseMessage(httpClient, messageUrl, createInitializedNotification());

            // Step 4: Test tools/list
            String toolsResponse = sendSseMessage(httpClient, messageUrl, createListToolsRequest(2));
            validateJsonRpcResponse(toolsResponse, 2);
            JsonNode toolsJson = objectMapper.readTree(toolsResponse);
            JsonNode tools = toolsJson.path("result").path("tools");
            assertTrue(tools.isArray() && tools.size() > 0, "Should have tools");
            boolean hasGenerateRandom = false;
            for (JsonNode tool : tools) {
                if ("generateRandom".equals(tool.path("name").asText())) {
                    hasGenerateRandom = true;
                    break;
                }
            }
            assertTrue(hasGenerateRandom, "generateRandom tool should be available");

            // Step 5: Test tools/call
            String toolCallResponse = sendSseMessage(httpClient, messageUrl, createCallToolRequest(3, "generateRandom", Map.of("bound", 50)));
            validateJsonRpcResponse(toolCallResponse, 3);
            JsonNode toolCallJson = objectMapper.readTree(toolCallResponse);
            JsonNode toolResult = toolCallJson.path("result");
            assertTrue(toolResult.has("content"), "Tool result should have content");
            JsonNode content = toolResult.path("content");
            assertTrue(content.isArray() && content.size() > 0, "Content should be array");
            int randomNumber = Integer.parseInt(content.get(0).path("text").asText());
            assertTrue(randomNumber >= 0 && randomNumber < 50, "Random number should be valid");

            // Step 6: Test prompts/list
            String promptsResponse = sendSseMessage(httpClient, messageUrl, createListPromptsRequest(4));
            validateJsonRpcResponse(promptsResponse, 4);
            JsonNode promptsJson = objectMapper.readTree(promptsResponse);
            JsonNode prompts = promptsJson.path("result").path("prompts");
            assertTrue(prompts.isArray() && prompts.size() >= 2, "Should have at least 2 prompts");

            // Step 7: Test prompts/get
            String promptGetResponse = sendSseMessage(httpClient, messageUrl, createGetPromptRequest(5, "random_game", Map.of("bound", 6)));
            validateJsonRpcResponse(promptGetResponse, 5);
            JsonNode promptGetJson = objectMapper.readTree(promptGetResponse);
            JsonNode promptResult = promptGetJson.path("result");
            assertTrue(promptResult.has("description"), "Prompt should have description");
            assertTrue(promptResult.has("messages"), "Prompt should have messages");

            // Step 8: Test resources/list
            String resourcesResponse = sendSseMessage(httpClient, messageUrl, createListResourcesRequest(6));
            validateJsonRpcResponse(resourcesResponse, 6);
            JsonNode resourcesJson = objectMapper.readTree(resourcesResponse);
            JsonNode resources = resourcesJson.path("result").path("resources");
            assertTrue(resources.isArray() && resources.size() > 0, "Should have resources");

            // Step 9: Test resources/read
            String resourceReadResponse = sendSseMessage(httpClient, messageUrl, createReadResourceRequest(7, "https://en.wikipedia.org/wiki/Random_number_generation"));
            validateJsonRpcResponse(resourceReadResponse, 7);
            JsonNode resourceReadJson = objectMapper.readTree(resourceReadResponse);
            JsonNode resourceContents = resourceReadJson.path("result").path("contents");
            assertTrue(resourceContents.isArray() && resourceContents.size() > 0, "Should have resource contents");

            // Step 10: Test templates/list
            String templatesResponse = sendSseMessage(httpClient, messageUrl, createListTemplatesRequest(8));
            validateJsonRpcResponse(templatesResponse, 8);
            JsonNode templatesJson = objectMapper.readTree(templatesResponse);
            JsonNode templates = templatesJson.path("result").path("resourceTemplates");
            assertTrue(templates.isArray() && templates.size() > 0, "Should have templates");

        } finally {
            httpServer.stop();
            mcpServer.shutdown();
        }
    }

    /**
     * Helper method to send a message via SSE and receive the response.
     */
    private String sendSseMessage(java.net.http.HttpClient httpClient, String messageUrl, String jsonRpcMessage) throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(messageUrl))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRpcMessage))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        // Check if this is a notification (no id field) - notifications return 202
        boolean isNotification = !jsonRpcMessage.contains("\"id\":");
        if (isNotification) {
            assertEquals(202, response.statusCode(), "SSE notification should return 202");
            return ""; // Notifications have no response body
        } else {
            assertEquals(200, response.statusCode(), "SSE request should return 200");
            // The response should be the JSON-RPC response
            String responseBody = response.body();
            assertTrue(responseBody.contains("jsonrpc"), "Response should be JSON-RPC message");
            return responseBody;
        }
    }

    /**
     * Test McpHttpServer with both SSE and Streamable HTTP transport.
     */
    @Test
    void testCombinedServerStartup() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = new StreamableServer();
        RandomService service = new RandomService();
        mcpServer.initialize(service);

        // Start McpHttpServer with both transports
        int port = 18082;
        McpHttpServer httpServer = new McpHttpServer(mcpServer, port);
        httpServer.startServer();

        McpSyncClient client = null;
        try {
            // Verify server is running
            assertTrue(httpServer.isRunning(), "Combined server should be running");

            // Test Streamable HTTP endpoint with MCP client
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                    .builder("http://localhost:" + port)
                    .endpoint("/mcp")
                    .build();

            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(10))
                    .initializationTimeout(Duration.ofSeconds(10))
                    .build();

            client.initialize();

            // Call the generateRandom tool
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest("generateRandom", Map.of("bound", 50)));

            assertNotNull(result, "Tool result should not be null");
            assertFalse(result.content().isEmpty(), "Result should have content");

            var content = result.content().get(0);
            assertTrue(content instanceof McpSchema.TextContent, "Content should be TextContent");
            
            String textResult = ((McpSchema.TextContent) content).text();
            int randomNumber = Integer.parseInt(textResult);
            assertTrue(randomNumber >= 0 && randomNumber < 50,
                    "Random number should be between 0 and 50, got: " + randomNumber);

            System.out.println("Generated random number (Combined server): " + randomNumber);

        } finally {
            if (client != null) {
                client.closeGracefully();
            }
            httpServer.stop();
            mcpServer.shutdown();
        }
    }

    /**
     * Comprehensive test for HTTP transport covering all MCP endpoints.
     *
     * This test uses direct HTTP requests to test prompts, resources, and templates
     * in addition to the existing tool coverage.
     */
    @Test
    void testHttpTransportComprehensive() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = createInitializedServer();

        // Start McpHttpServer with Streamable HTTP transport
        int port = 18084;
        McpHttpServer httpServer = new McpHttpServer(mcpServer, port);
        httpServer.startStreamableServer();

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

        try {
            // Step 1: Test initialize
            String initResponse = sendHttpRequest(httpClient, port, createInitializeRequest(1));
            validateJsonRpcResponse(initResponse, 1);
            JsonNode initJson = objectMapper.readTree(initResponse);
            assertEquals("2024-11-05", initJson.path("result").path("protocolVersion").asText());
            assertEquals("mcp-random-server", initJson.path("result").path("serverInfo").path("name").asText());

            // Step 2: Send initialized notification (notifications return 200 with empty body)
            java.net.http.HttpRequest initNotificationRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:" + port + "/mcp"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(createInitializedNotification()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpResponse<String> initNotificationResponse = httpClient.send(initNotificationRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, initNotificationResponse.statusCode(), "Initialized notification should return 200");
            assertTrue(initNotificationResponse.body().isEmpty() || initNotificationResponse.body().trim().isEmpty(),
                    "Notification response should have empty body");

            // Step 3: Test tools/list (already covered in existing tests, but let's verify)
            String toolsResponse = sendHttpRequest(httpClient, port, createListToolsRequest(2));
            validateJsonRpcResponse(toolsResponse, 2);
            JsonNode toolsJson = objectMapper.readTree(toolsResponse);
            JsonNode tools = toolsJson.path("result").path("tools");
            assertTrue(tools.isArray() && tools.size() > 0, "Should have tools");

            // Step 4: Test tools/call (already covered, but verify)
            String toolCallResponse = sendHttpRequest(httpClient, port, createCallToolRequest(3, "generateRandom", Map.of("bound", 25)));
            validateJsonRpcResponse(toolCallResponse, 3);
            JsonNode toolCallJson = objectMapper.readTree(toolCallResponse);
            JsonNode toolResult = toolCallJson.path("result");
            JsonNode content = toolResult.path("content");
            int randomNumber = Integer.parseInt(content.get(0).path("text").asText());
            assertTrue(randomNumber >= 0 && randomNumber < 25, "Random number should be valid");

            // Step 5: Test prompts/list
            String promptsResponse = sendHttpRequest(httpClient, port, createListPromptsRequest(4));
            validateJsonRpcResponse(promptsResponse, 4);
            JsonNode promptsJson = objectMapper.readTree(promptsResponse);
            JsonNode prompts = promptsJson.path("result").path("prompts");
            assertTrue(prompts.isArray() && prompts.size() >= 2, "Should have at least 2 prompts");

            // Verify prompt names
            boolean hasRandomGame = false;
            boolean hasRandomTest = false;
            for (JsonNode prompt : prompts) {
                String name = prompt.path("name").asText();
                if ("random_game".equals(name)) hasRandomGame = true;
                if ("random_test".equals(name)) hasRandomTest = true;
            }
            assertTrue(hasRandomGame && hasRandomTest, "Should have both random_game and random_test prompts");

            // Step 6: Test prompts/get for random_game
            String promptGetResponse = sendHttpRequest(httpClient, port, createGetPromptRequest(5, "random_game", Map.of("bound", 6)));
            validateJsonRpcResponse(promptGetResponse, 5);
            JsonNode promptGetJson = objectMapper.readTree(promptGetResponse);
            JsonNode promptResult = promptGetJson.path("result");
            assertTrue(promptResult.has("description"), "Prompt should have description");
            assertTrue(promptResult.has("messages"), "Prompt should have messages");
            assertTrue(promptResult.path("description").asText().contains("gaming scenarios"), "Should be gaming prompt");

            // Step 7: Test prompts/get for random_test
            String promptTestResponse = sendHttpRequest(httpClient, port, createGetPromptRequest(6, "random_test", Map.of("bound", 1000)));
            validateJsonRpcResponse(promptTestResponse, 6);
            JsonNode promptTestJson = objectMapper.readTree(promptTestResponse);
            JsonNode promptTestResult = promptTestJson.path("result");
            assertTrue(promptTestResult.has("description"), "Test prompt should have description");
            assertTrue(promptTestResult.path("description").asText().contains("testing scenarios"), "Should be testing prompt");

            // Step 8: Test resources/list
            String resourcesResponse = sendHttpRequest(httpClient, port, createListResourcesRequest(7));
            validateJsonRpcResponse(resourcesResponse, 7);
            JsonNode resourcesJson = objectMapper.readTree(resourcesResponse);
            JsonNode resources = resourcesJson.path("result").path("resources");
            assertTrue(resources.isArray() && resources.size() > 0, "Should have resources");

            // Verify the wikipedia resource
            boolean hasWikipediaResource = false;
            for (JsonNode resource : resources) {
                if ("https://en.wikipedia.org/wiki/Random_number_generation".equals(resource.path("uri").asText())) {
                    hasWikipediaResource = true;
                    assertEquals("Information about Random Number Generation", resource.path("title").asText());
                    break;
                }
            }
            assertTrue(hasWikipediaResource, "Should have wikipedia resource");

            // Step 9: Test resources/read
            String resourceReadResponse = sendHttpRequest(httpClient, port, createReadResourceRequest(8, "https://en.wikipedia.org/wiki/Random_number_generation"));
            validateJsonRpcResponse(resourceReadResponse, 8);
            JsonNode resourceReadJson = objectMapper.readTree(resourceReadResponse);
            JsonNode resourceContents = resourceReadJson.path("result").path("contents");
            assertTrue(resourceContents.isArray() && resourceContents.size() > 0, "Should have resource contents");

            // Verify content structure
            JsonNode firstContent = resourceContents.get(0);
            assertTrue(firstContent.has("uri"), "Content should have URI");
            assertTrue(firstContent.has("mimeType"), "Content should have mimeType");
            assertEquals("text/html", firstContent.path("mimeType").asText());

            // Step 10: Test templates/list
            String templatesResponse = sendHttpRequest(httpClient, port, createListTemplatesRequest(9));
            validateJsonRpcResponse(templatesResponse, 9);
            JsonNode templatesJson = objectMapper.readTree(templatesResponse);
            JsonNode templates = templatesJson.path("result").path("resourceTemplates");
            assertTrue(templates.isArray() && templates.size() > 0, "Should have templates");

            // Verify the project files template
            boolean hasProjectFilesTemplate = false;
            for (JsonNode template : templates) {
                if ("file:///{path}".equals(template.path("uriTemplate").asText())) {
                    hasProjectFilesTemplate = true;
                    assertEquals("Project Files", template.path("name").asText());
                    assertEquals("📁 Project Files", template.path("title").asText());
                    break;
                }
            }
            assertTrue(hasProjectFilesTemplate, "Should have project files template");

            // Step 11: Test templates/read
            String templateReadResponse = sendHttpRequest(httpClient, port, createReadTemplateRequest(10, "file:///{path}"));
            validateJsonRpcResponse(templateReadResponse, 10);
            JsonNode templateReadJson = objectMapper.readTree(templateReadResponse);
            JsonNode templateContents = templateReadJson.path("result").path("contents");
            assertTrue(templateContents.isArray() && templateContents.size() > 0, "Should have template contents");

        } finally {
            httpServer.stop();
            mcpServer.shutdown();
        }
    }

    /**
     * Helper method to send HTTP request and return response body.
     */
    private String sendHttpRequest(java.net.http.HttpClient httpClient, int port, String jsonRpcMessage) throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/mcp"))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonRpcMessage))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "HTTP request should return 200");

        String responseBody = response.body();
        assertTrue(responseBody.contains("jsonrpc"), "Response should be JSON-RPC message");
        return responseBody;
    }
}

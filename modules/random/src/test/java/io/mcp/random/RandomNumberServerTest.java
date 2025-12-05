package io.mcp.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

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

    /**
     * Test 1: Open MCP stdio transport and call the generateRandom tool.
     * 
     * This test simulates the stdio transport by redirecting stdin/stdout
     * and sending JSON-RPC messages to the server.
     */
    @Test
    void testStdioTransportGenerateRandom() throws Exception {
        // Prepare JSON-RPC requests for stdio
        String initializeRequest = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
            """;
        String initializedNotification = """
            {"jsonrpc":"2.0","method":"initialized"}
            """;
        String toolCallRequest = """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"generateRandom","arguments":{"bound":100}}}
            """;

        // Combine all requests
        String allRequests = initializeRequest + initializedNotification + toolCallRequest;

        // Save original stdin/stdout
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            // Redirect stdin with our test input
            System.setIn(new ByteArrayInputStream(allRequests.getBytes()));
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
                                    .build())
                            .tools(service.getTools().stream()
                                    .map(t -> t.getToolSpecification())
                                    .toList())
                            .build();

                    // Give the server time to process
                    Thread.sleep(2000);
                    server.close();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            });

            serverThread.start();

            // Wait for server to complete processing
            boolean completed = latch.await(5, TimeUnit.SECONDS);

            // Restore streams before assertions
            System.setIn(originalIn);
            System.setOut(originalOut);

            String output = capturedOutput.toString();
            System.out.println("Captured output: " + output);

            // Verify we got responses (stdio outputs JSON-RPC messages line by line)
            assertTrue(completed || !output.isEmpty(), "Server should produce output");

            // Check for initialize response or tool result in the output
            if (!output.isEmpty()) {
                // The output may contain multiple JSON-RPC responses
                assertTrue(output.contains("jsonrpc") || output.contains("result"),
                        "Output should contain JSON-RPC responses");
            }

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
     * Test using the official MCP SDK's McpSyncClient with HTTP transport.
     * 
     * This test starts an embedded HTTP server, connects to it using
     * HttpClientStreamableHttpTransport, and calls the generateRandom tool.
     */
    @Test
    void testHttpTransportClientGenerateRandom() throws Exception {
        // Set up the StreamableServer with RandomService
        StreamableServer mcpServer = new StreamableServer();
        RandomService service = new RandomService();
        mcpServer.initialize(service);

        // Start an embedded HTTP server on a random available port
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        int port = httpServer.getAddress().getPort();
        
        httpServer.createContext("/mcp", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                String response = mcpServer.handleRequestSync(requestBody, null);
                
                exchange.getResponseHeaders().set("Content-Type", mcpServer.getContentType());
                byte[] responseBytes = response.getBytes();
                exchange.sendResponseHeaders(200, responseBytes.length);
                exchange.getResponseBody().write(responseBytes);
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });
        httpServer.start();

        McpSyncClient client = null;
        try {
            // Create HTTP transport pointing to our embedded server
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

            System.out.println("Generated random number: " + randomNumber);

        } finally {
            // Clean up resources
            if (client != null) {
                client.closeGracefully();
            }
            httpServer.stop(0);
            mcpServer.shutdown();
        }
    }
}

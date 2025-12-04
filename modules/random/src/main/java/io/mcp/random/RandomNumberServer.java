package io.mcp.random;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class RandomNumberServer {

    private static final Random random = new Random();

    public static void main(String[] args) {
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("mcp-random-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(generateRandomTool())
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));
    }

    private static String getPrefix() {
        System.err.println("getPrefix");
        return "V6-HOT-RELOADED-";
    }   

    private static McpServerFeatures.SyncToolSpecification generateRandomTool() {
        var inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "bound", Map.of(
                                "type", "integer",
                                "description", "The upper bound (exclusive) for the random number. Must be positive."
                        )
                ),
                List.of("bound"),
                null,
                null,
                null
        );

        var tool = McpSchema.Tool.builder()
                .name("generateRandom")
                .description("Generates a random integer between 0 (inclusive) and the specified bound (exclusive)")
                .inputSchema(inputSchema)
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    int bound = ((Number) request.arguments().get("bound")).intValue();

                    if (bound <= 0) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("Error: bound must be a positive integer")
                                .isError(true)
                                .build();
                    }

                    int result = random.nextInt(bound);

                    return McpSchema.CallToolResult.builder()
                            .addTextContent(getPrefix() + result)
                            .isError(false)
                            .build();
                })
                .build();
    }
}

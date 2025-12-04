package io.mcp.random;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.random.service.RandomService;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class RandomNumberServer {

    public static void main(String[] args) {
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        RandomService randomService = new RandomService();

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("mcp-random-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(randomService.createRandomTool())
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));
    }

}

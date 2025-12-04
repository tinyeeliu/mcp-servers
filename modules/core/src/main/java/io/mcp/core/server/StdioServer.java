package io.mcp.core.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class StdioServer {
    

    public void start(McpService mcpService){


        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        List<McpTool> tools = mcpService.getTools();

        List<McpServerFeatures.SyncToolSpecification> toolSpecifications = new ArrayList<>();

        for (McpTool tool : tools) {
            toolSpecifications.add(tool.getToolSpecification());
        }

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo(mcpService.getServerInfo())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(toolSpecifications)
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));

    }

}
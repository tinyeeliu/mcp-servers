package io.mcp.core.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.utility.Utility;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class SimpleSdkStdioSyncTestServer {

    public void start() {
        // Create JSON mapper and transport provider
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // Empty lists for all capabilities (no tools, prompts, resources, or templates)
        List<McpServerFeatures.SyncToolSpecification> toolSpecifications = new ArrayList<>();
        List<McpServerFeatures.SyncPromptSpecification> promptSpecifications = new ArrayList<>();
        List<McpServerFeatures.SyncResourceSpecification> resourceSpecifications = new ArrayList<>();
        List<McpServerFeatures.SyncResourceTemplateSpecification> templateSpecifications = new ArrayList<>();

        // Create server with no capabilities - just connection testing
        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo(new McpSchema.Implementation("SimpleSdkStdioSyncTestServer", "1.0.0"))
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(false)
                        .prompts(false)
                        .resources(false, false)
                        .build())
                .tools(toolSpecifications)
                .prompts(promptSpecifications)
                .resources(resourceSpecifications)
                .resourceTemplates(templateSpecifications)
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Utility.debug("Shutting down SimpleSdkStdioSyncTestServer");
            server.close();
        }));

        Utility.debug("SimpleSdkStdioSyncTestServer started - connection test mode");
    }

    public static void main(String[] args) {
        // Redirect stderr to log file to capture all error output
        Utility.redirectStdErrToLog();

        Utility.debug("SimpleSdkStdioSyncTestServer starting...");
        SimpleSdkStdioSyncTestServer server = new SimpleSdkStdioSyncTestServer();
        server.start();

        // Keep the main thread alive indefinitely for stdio communication
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Utility.debug("Server interrupted, shutting down");
        }
    }

}

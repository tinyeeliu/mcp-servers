package io.mcp.core.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.utility.Utility;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class SimpleSdkStdioAsyncTestServer {

    public void start() {
        // Create JSON mapper and transport provider
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        // Empty lists for all capabilities (no tools, prompts, resources, or templates)
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = new ArrayList<>();
        List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = new ArrayList<>();
        List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications = new ArrayList<>();
        List<McpServerFeatures.AsyncResourceTemplateSpecification> templateSpecifications = new ArrayList<>();

        // Create server with no capabilities - just connection testing
        McpAsyncServer server = McpServer.async(transportProvider)
                .serverInfo(new McpSchema.Implementation("SimpleSdkStdioTestServer", "1.0.0"))
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
            Utility.debug("Shutting down SimpleSdkStdioTestServer");
            server.close();
        }));

        Utility.debug("SimpleSdkStdioTestServer started - connection test mode");
    }

    public static void main(String[] args) {
        // Redirect stderr to log file to capture all error output
        Utility.redirectStdErrToLog();
https://github.com/modelcontextprotocol/java-sdk
        Utility.debug("SimpleSdkStdioAsyncTestServer starting...");
        SimpleSdkStdioAsyncTestServer server = new SimpleSdkStdioAsyncTestServer();
        server.start();
    }

}

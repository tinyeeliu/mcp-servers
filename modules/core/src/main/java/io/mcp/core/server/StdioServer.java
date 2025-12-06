package io.mcp.core.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.Utility;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class StdioServer {
    

    public void start(McpService mcpService){


        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        List<McpTool> tools = mcpService.getTools();

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = new ArrayList<>();

        for (McpTool tool : tools) {
            toolSpecifications.add(tool.getToolSpecification());
        }

        List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = mcpService.getPromptSpecifications();

        List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications = mcpService.getResourceSpecifications();

        List<McpServerFeatures.AsyncResourceTemplateSpecification> templateSpecifications = mcpService.getResourceTemplateSpecifications();

        McpAsyncServer server = McpServer.async(transportProvider)
                .serverInfo(mcpService.getServerInfo())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .prompts(true)
                        .resources(true, false)
                        .build())
                .tools(toolSpecifications)
                .prompts(promptSpecifications)
                .resources(resourceSpecifications)
                .resourceTemplates(templateSpecifications)
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Utility.debug("Shutting down server");
            server.close();
        }));
        

        

    }

}
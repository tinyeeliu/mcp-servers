package io.mcp.core.server;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

public class StdioServer {
    

    public void start(McpService mcpService){


        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        List<McpTool> tools = mcpService.getTools();

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = new ArrayList<>();

        for (McpTool tool : tools) {
            toolSpecifications.add(tool.getToolSpecification());
        }


        // Create a dummy prompt specification
        List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = new ArrayList<>();
        promptSpecifications.add(new McpServerFeatures.AsyncPromptSpecification(
                new McpSchema.Prompt("dummy_prompt", "A dummy prompt for testing", List.of()),
                (exchange, request) -> {
                    // Dummy prompt handler - returns a simple message
                    return Mono.just(
                        new McpSchema.GetPromptResult("Dummy prompt result", List.of(
                            new McpSchema.PromptMessage(
                                McpSchema.Role.USER,
                                new McpSchema.TextContent("This is a dummy prompt response")
                            )
                        ))
                    );
                }
        ));

        McpAsyncServer server = McpServer.async(transportProvider)
                .serverInfo(mcpService.getServerInfo())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .prompts(true)
                        .build())
                .tools(toolSpecifications)
                .prompts(promptSpecifications)
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));

    }

}
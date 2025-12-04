package io.mcp.random.service;

import io.mcp.core.base.BaseMcpService;
import io.mcp.random.tool.GenerateRandom;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

public class RandomService extends BaseMcpService{

    private final GenerateRandom generateRandom = new GenerateRandom();

    @Override
    public Implementation getServerInfo() {
        Implementation result = new Implementation("mcp-random-server", "1.0.0");
        return result;
    }

    public McpServerFeatures.SyncToolSpecification createRandomTool() {
        var tool = generateRandom.getTool();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        int bound = ((Number) request.arguments().get("bound")).intValue();
                        String result = generateRandom.call(bound);

                        return McpSchema.CallToolResult.builder()
                                .addTextContent(result)
                                .isError(false)
                                .build();
                    } catch (IllegalArgumentException e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("Error: " + e.getMessage())
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}

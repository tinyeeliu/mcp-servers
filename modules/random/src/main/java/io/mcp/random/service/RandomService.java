package io.mcp.random.service;

import java.util.List;

import io.mcp.core.base.BaseMcpService;
import io.mcp.core.protocol.McpTool;
import io.mcp.random.tool.GenerateRandom;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

public class RandomService extends BaseMcpService{

  
    @Override
    public Implementation getServerInfo() {
        Implementation result = new Implementation("mcp-random-server", "1.0.0");
        return result;
    }

    
    public McpServerFeatures.SyncToolSpecification createRandomTool() {
        GenerateRandom generateRandom = new GenerateRandom();
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

    @Override
    public List<McpTool> getTools() {
        return List.of(new GenerateRandom());
    }
}

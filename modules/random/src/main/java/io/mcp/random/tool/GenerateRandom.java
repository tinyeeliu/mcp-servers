package io.mcp.random.tool;

import java.util.List;
import java.util.Map;
import java.util.Random;

import io.mcp.core.base.BaseMcpTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

public class GenerateRandom extends BaseMcpTool {

    private static final Random random = new Random();

    private String makePrefix() {
        return "V6-HOT-RELOADED-";
    }

    @Override
    public String call(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be a positive integer");
        }
        int result = random.nextInt(bound);
        return makePrefix() + result;
    }

    @Override
    public McpSchema.JsonSchema getInputSchema() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
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
        return inputSchema;
    }

    @Override
    public McpSchema.Tool getTool() {
        return McpSchema.Tool.builder()
                .name("generateRandom")
                .description("Generates a random integer between 0 (inclusive) and the specified bound (exclusive)")
                .inputSchema(getInputSchema())
                .build();
    }

    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
       
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(getTool())
                .callHandler((exchange, request) -> {
                    try {
                        int bound = ((Number) request.arguments().get("bound")).intValue();
                        String result = call(bound);

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

package io.mcp.random.tool;

import java.util.List;
import java.util.Map;
import java.util.Random;

import io.modelcontextprotocol.spec.McpSchema;

public class GenerateRandom {

    private static final Random random = new Random();

    private String makePrefix() {
        return "V6-HOT-RELOADED-";
    }

    public String call(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be a positive integer");
        }
        int result = random.nextInt(bound);
        return makePrefix() + result;
    }

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

    public McpSchema.Tool getTool() {
        return McpSchema.Tool.builder()
                .name("generateRandom")
                .description("Generates a random integer between 0 (inclusive) and the specified bound (exclusive)")
                .inputSchema(getInputSchema())
                .build();
    }
}

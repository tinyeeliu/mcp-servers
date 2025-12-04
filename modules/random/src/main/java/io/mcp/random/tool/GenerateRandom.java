package io.mcp.random.tool;

import java.io.IOException;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.base.BaseMcpTool;
import io.mcp.core.utility.JsonSchemaUtility;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

public class GenerateRandom extends BaseMcpTool {

    private static final Random random = new Random();


    @Override
    public String getName() {
        return "generateRandom";
    }


    private String makePrefix() {
        return "V6-";
    }

    @Override
    public McpSchema.CallToolResult call(McpSyncServerExchange exchange, CallToolRequest request) {

        int bound = ((Number) request.arguments().get("bound")).intValue();

        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be a positive integer");
        }
        int result = random.nextInt(bound);
        return McpSchema.CallToolResult.builder()
                .addTextContent(makePrefix() + result)
                .isError(false)
                .build();
    }
    
    
    @Override
    public McpSchema.Tool getTool() {

        try {
            String jsonSchema = JsonSchemaUtility.loadJsonSchema("io/mcp/random/tool/" + getName() + ".json");
            JsonNode jsonNode = JsonSchemaUtility.toJsonNode(jsonSchema);
            return JsonSchemaUtility.getTool(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tool specification", e);
        }


    }



    /* 
    //@Override
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
                .name(getName())
                .description("Generates a random integer between 0 (inclusive) and the specified bound (exclusive)")
                .inputSchema(getInputSchema())
                .build();
    }*/


}

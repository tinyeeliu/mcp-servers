package io.mcp.random.tool;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.base.BaseMcpTool;
import io.mcp.core.utility.JsonSchemaUtility;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

public class GenerateRandom extends BaseMcpTool {

    private static final Random random = new Random();


    @Override
    public String getName() {
        return "generateRandom";
    }


    private String makePrefix() {
        return "";
    }

    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request) {

        int bound = ((Number) request.arguments().get("bound")).intValue();

        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be a positive integer");
        }
        int result = random.nextInt(bound);
        return CompletableFuture.completedFuture(
            McpSchema.CallToolResult.builder()
                .addTextContent(makePrefix() + result)
                .isError(false)
                .build()
        );
    }

 
    @Override
    public McpSchema.Tool getTool() {

        try {
            String jsonSchema = JsonSchemaUtility.loadJsonSchema("io/mcp/spec/tool/" + getName() + ".json");
            JsonNode jsonNode = JsonSchemaUtility.toJsonNode(jsonSchema);
            return JsonSchemaUtility.getTool(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tool specification", e);
        }


    }


    @Override
    public List<McpServerFeatures.AsyncPromptSpecification> getPromptSpecifications() {

        try {
            String jsonSchema = JsonSchemaUtility.loadJsonSchema("io/mcp/spec/prompt/" + getName() + ".json");
            JsonNode jsonNode = JsonSchemaUtility.toJsonNode(jsonSchema);
            return JsonSchemaUtility.getPrompts(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt specification", e);
        }


    }

}

package io.mcp.random.tool;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.base.BaseMcpTool;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
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

 


}

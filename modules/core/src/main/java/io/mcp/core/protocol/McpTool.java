package io.mcp.core.protocol;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public interface McpTool {

    //public McpSchema.JsonSchema getInputSchema();
    public McpSchema.Tool getTool();
    public McpServerFeatures.AsyncToolSpecification getToolSpecification();

    public List<McpServerFeatures.AsyncPromptSpecification> getPromptSpecifications() ;
    public CompletableFuture<CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request);
    public String getName();
}

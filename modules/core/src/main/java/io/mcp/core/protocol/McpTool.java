package io.mcp.core.protocol;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public interface McpTool {
   
    //public McpSchema.JsonSchema getInputSchema();
    public McpSchema.Tool getTool();
    public McpServerFeatures.SyncToolSpecification getToolSpecification();
    public CallToolResult call(McpSyncServerExchange exchange, CallToolRequest request);
    public String getName();
}

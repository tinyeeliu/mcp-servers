package io.mcp.core.protocol;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

public interface McpTool {
    public String call(int bound);
    public McpSchema.JsonSchema getInputSchema();
    public McpSchema.Tool getTool();
    public McpServerFeatures.SyncToolSpecification getToolSpecification();
}

package io.mcp.core.base;

import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

public abstract class BaseMcpTool implements McpTool {
    @Override
    public McpServerFeatures.SyncToolSpecification getToolSpecification() {
       
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(getTool())
                .callHandler((exchange, request) -> {
                    try {
                        return call(exchange, request);
                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(e.getMessage())
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}

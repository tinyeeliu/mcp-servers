package io.mcp.core.protocol;

import io.modelcontextprotocol.spec.McpSchema.Implementation;

public interface McpService {
    public Implementation getServerInfo();
}
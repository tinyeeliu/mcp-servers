package io.mcp.core.protocol;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.Implementation;

public interface McpService {
    public Implementation getServerInfo();
    public List<McpTool> getTools();
}
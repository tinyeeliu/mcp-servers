package io.mcp.core.protocol;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/*

A HTTP post command for specific endpoint. It's not a MCP service.

*/

public interface McpCommand {
    

    public CompletableFuture<Map<String, Object>> execute();
}

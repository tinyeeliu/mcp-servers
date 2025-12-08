package io.mcp.core.command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.protocol.McpCommand;

public class WarmupCommand implements McpCommand{

    @Override
    public CompletableFuture<Map<String, Object>> execute() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        return CompletableFuture.completedFuture(result);
    }
    
}

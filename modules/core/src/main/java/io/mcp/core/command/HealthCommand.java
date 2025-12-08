package io.mcp.core.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.protocol.McpCommand;

public class HealthCommand implements McpCommand{

    @Override
    public CompletableFuture<Map<String, Object>> execute() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");

        List<String> checks = new ArrayList<>();
        result.put("checks", checks);


        return CompletableFuture.completedFuture(result);
    }
    
}

package io.mcp.core.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.protocol.McpCommand;
import io.mcp.core.server.McpHttpServer;

/*

{
    "status": "UP",
    "checks": [
        {
            "name": "Redis connection health check",
            "status": "UP",
            "data": {
                "redis-0": "PONG",
                "redis-1": "PONG"
            }
        },
        {
            "name": "Aigens App Ready",
            "status": "UP"
        }
    ]
}

*/

public class HealthCommand implements McpCommand{

    @Override
    public CompletableFuture<Map<String, Object>> execute() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");

        List<Map<String, Object>> checks = new ArrayList<>();

        // Add HTTP server health check
        McpHttpServer server = McpHttpServer.getCurrentInstance();
        if (server != null) {
            Map<String, Object> serverCheck = new HashMap<>();
            Map<String, Object> serverHealth = server.getServerHealth();

            serverCheck.put("name", "HTTP Server Health Check");
            serverCheck.put("status", serverHealth.get("status"));
            serverCheck.put("data", serverHealth);

            checks.add(serverCheck);

            // If server is down, set overall status to DOWN
            if ("DOWN".equals(serverHealth.get("status"))) {
                result.put("status", "DOWN");
            }
        }

        result.put("checks", checks);

        return CompletableFuture.completedFuture(result);
    }
    
}

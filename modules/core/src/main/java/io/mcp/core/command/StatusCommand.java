package io.mcp.core.command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.protocol.McpCommand;
import io.mcp.core.utility.Utility;

public class StatusCommand implements McpCommand{

    @Override
    public CompletableFuture<Map<String, Object>> execute() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("debug", Utility.isDebug());
        result.put("native", Utility.isNative());
        result.put("errorRedirected", Utility.isErrorRedirected());
        result.put("port", Utility.getConfiguredPort());
        result.put("fileLogging", Utility.isFileLogging());
        result.put("version", "1.0.1");
        return CompletableFuture.completedFuture(result);
    }
    
}

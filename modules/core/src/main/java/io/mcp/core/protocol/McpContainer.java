package io.mcp.core.protocol;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface McpContainer {

    /*
    Send the log to container to be processed.

     */
    public void log(System.Logger.Level level, Object... msgs);
    public void log(String message, Throwable e);

    CompletableFuture<Map<String, Object>> getAuthInfo(String sessionId, String module);
}

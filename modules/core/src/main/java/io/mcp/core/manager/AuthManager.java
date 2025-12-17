package io.mcp.core.manager;

import io.mcp.core.protocol.McpContainer;
import io.mcp.core.utility.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/*

DO NOT IMPLEMENT THIS CLASS YET.

This class will retrieve the preauthenticated information for the session.
For example, Google Calendar auth token for each specific user to access the Google Calendar API.

*/

public class AuthManager {

    public CompletableFuture<Map<String, Object>> getAuthInfo(String sessionId, String module){

        McpContainer mcpContainer = Utility.getMcpContainer();

        if(mcpContainer == null){
            Map<String, Object> info = new HashMap<>();
            info.put("authToken", sessionId);
            return CompletableFuture.completedFuture(info);
        }

        return mcpContainer.getAuthInfo(sessionId, module);


    }
}

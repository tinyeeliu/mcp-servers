package io.mcp.core.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.manager.AuthManager;
import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.Utility;
import io.modelcontextprotocol.server.McpServerFeatures;

import static io.mcp.core.utility.Utility.debug;

public abstract class BaseMcpService implements McpService {

    private AuthManager authManager;

    public BaseMcpService(){
        this.authManager = new AuthManager();
    }

    @Override
    public List<McpServerFeatures.AsyncPromptSpecification> getPromptSpecifications(){
        List<McpTool> tools = getTools();
        List<McpServerFeatures.AsyncPromptSpecification> allPromptSpecifications = new ArrayList<>();
        for (McpTool tool : tools) {
            List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = tool.getPromptSpecifications();
            allPromptSpecifications.addAll(promptSpecifications);
        }
        return allPromptSpecifications;
    }

    @Override
    public List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications(){
        List<McpTool> tools = getTools();
        List<McpServerFeatures.AsyncResourceSpecification> allResourceSpecifications = new ArrayList<>();
        for (McpTool tool : tools) {
            List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications = tool.getResourceSpecifications();
            allResourceSpecifications.addAll(resourceSpecifications);
        }
        return allResourceSpecifications;
    }

    @Override
    public List<McpServerFeatures.AsyncResourceTemplateSpecification> getResourceTemplateSpecifications(){
        List<McpTool> tools = getTools();
        List<McpServerFeatures.AsyncResourceTemplateSpecification> allResourceTemplateSpecifications = new ArrayList<>();
        for (McpTool tool : tools) {
            List<McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplateSpecifications = tool.getResourceTemplateSpecifications();
            allResourceTemplateSpecifications.addAll(resourceTemplateSpecifications);
        }
        return allResourceTemplateSpecifications;
    }

    public CompletableFuture<String> fetchAuthToken(String sessionId) {

        return authManager.getAuthInfo(sessionId, getModule())
                .thenApply(map -> {
                    String token = (String) map.get("authToken");
                    if (token == null) {
                        throw new RuntimeException("No authToken available for session");
                    }
                    return token;
                });
    }
}

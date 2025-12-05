package io.mcp.core.base;

import java.util.ArrayList;
import java.util.List;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures;

public abstract class BaseMcpService implements McpService {
    
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
}
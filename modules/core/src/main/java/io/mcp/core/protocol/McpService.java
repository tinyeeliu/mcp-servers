package io.mcp.core.protocol;

import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

public interface McpService {
    public Implementation getServerInfo();
    public List<McpTool> getTools();
    public List<McpServerFeatures.AsyncPromptSpecification> getPromptSpecifications();
    public List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications();
    public List<McpServerFeatures.AsyncResourceTemplateSpecification> getResourceTemplateSpecifications();
}
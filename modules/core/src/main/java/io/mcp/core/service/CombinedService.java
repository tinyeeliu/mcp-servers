package io.mcp.core.service;

import java.util.List;

import io.mcp.core.protocol.McpService;
import io.mcp.core.protocol.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema.Implementation;

/*

This service combines multiple services into one.
This will be served at the root path /mcp.



*/

public class CombinedService implements McpService{


    private List<McpService> services;
    public CombinedService(List<McpService> services) {
        this.services = services;
    }

    @Override
    public Implementation getServerInfo() {
        Implementation result = new Implementation("mcp-server", "1.0.0");
        return result;
    }

    @Override
    public List<McpTool> getTools() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTools'");
    }

    @Override
    public String getModule() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getModule'");
    }

    @Override
    public List<AsyncPromptSpecification> getPromptSpecifications() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPromptSpecifications'");
    }

    @Override
    public List<AsyncResourceSpecification> getResourceSpecifications() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResourceSpecifications'");
    }

    @Override
    public List<AsyncResourceTemplateSpecification> getResourceTemplateSpecifications() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResourceTemplateSpecifications'");
    }
    
}

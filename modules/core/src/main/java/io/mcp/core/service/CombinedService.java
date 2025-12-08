package io.mcp.core.service;

import java.util.List;
import java.util.stream.Collectors;

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
        return services.stream()
                .flatMap(service -> service.getTools().stream())
                .collect(Collectors.toList());
    }

    @Override
    public String getModule() {
        return "root";
    }

    @Override
    public List<AsyncPromptSpecification> getPromptSpecifications() {
        return services.stream()
                .flatMap(service -> service.getPromptSpecifications().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<AsyncResourceSpecification> getResourceSpecifications() {
        return services.stream()
                .flatMap(service -> service.getResourceSpecifications().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<AsyncResourceTemplateSpecification> getResourceTemplateSpecifications() {
        return services.stream()
                .flatMap(service -> service.getResourceTemplateSpecifications().stream())
                .collect(Collectors.toList());
    }
    
}

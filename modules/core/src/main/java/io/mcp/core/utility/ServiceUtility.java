package io.mcp.core.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.mcp.core.protocol.McpService;

public class ServiceUtility {
    
    public static List<McpService> getRegisteredServices() {
        ServiceLoader<McpService> featureLoader = ServiceLoader.load(McpService.class);
        Utility.debug("Registered MCP Services:");
        
        List<McpService> services = new ArrayList<>();

        for (McpService feature : featureLoader) {
            Utility.debug(feature.getClass().getName());
            services.add(feature);
        }
        return services;
        
    }
}


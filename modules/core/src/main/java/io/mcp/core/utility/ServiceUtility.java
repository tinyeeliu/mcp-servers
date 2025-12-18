package io.mcp.core.utility;

import java.util.*;

import io.mcp.core.protocol.McpService;
import io.mcp.core.server.StreamableServer;

public class ServiceUtility {

    private static Map<String, McpService> serviceMap = null;
    private static List<McpService> serviceList = null;
    private static Map<String, StreamableServer> serverMap = null;

    public static List<McpService> getRegisteredServices() {
        init();
        return serviceList;
    }

    public static McpService getService(String module) {
        init();
        return serviceMap.get(module);
    }

    public static StreamableServer getServer(String module){

        init();

        StreamableServer mcpServer = serverMap.get(module);

        if(mcpServer == null) {
            Utility.debug("Making new MCP Server for " + module);
            McpService service = getService(module);
            mcpServer = new StreamableServer();
            mcpServer.initialize(service);

            serverMap.put(module, mcpServer);
        }



        return mcpServer;
    }

    private static void init() {

        if(serviceMap != null) {
            return;
        }

        ServiceLoader<McpService> featureLoader = ServiceLoader.load(McpService.class);
        Utility.debug("Registered MCP Services", featureLoader);

        List<McpService> list = new ArrayList<>();
        Map<String, McpService> map = new HashMap<>();

        int count = 0;

        for (McpService feature : featureLoader) {
            Utility.debug(feature.getClass().getName());
            list.add(feature);
            map.put(feature.getModule(), feature);
            count++;
        }

        Utility.debug("loaded services count", count);

        serviceList = list;
        serviceMap = map;

        serverMap = new HashMap<>();

    }
}


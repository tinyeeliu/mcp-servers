package io.mcp.random;

import io.mcp.core.protocol.McpService;
import io.mcp.core.server.StandaloneServer;
import io.mcp.random.service.RandomService;

public class RandomNumberServer {

    public static void main(String[] args) throws Exception {

        String transport = args[0];
        McpService service = new RandomService();
        StandaloneServer.launch(transport, service);

    }

}

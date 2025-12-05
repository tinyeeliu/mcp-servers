package io.mcp.random;

import io.mcp.core.protocol.McpService;
import io.mcp.core.server.StandaloneServer;
import io.mcp.random.service.RandomService;

public class RandomNumberServer {

    public static void main(String[] args) throws Exception {

        String transport = args[0];
        McpService service = new RandomService();
        StandaloneServer.launch(transport, service);

        /* 
        var jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        var transportProvider = new StdioServerTransportProvider(jsonMapper);

        RandomService randomService = new RandomService();

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("mcp-random-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(randomService.createRandomTool())
                .build();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));*/
        /* 
        
        StdioServer stdioServer = new StdioServer();
        stdioServer.start(new RandomService());*/
    }

}

package io.mcp.core.server;

import java.io.IOException;

import io.mcp.core.protocol.McpService;

/**
 * Legacy StdioServer - now delegates to the new native-image compatible McpStdioServer.
 */
public class StdioServer {

    public void start(McpService mcpService) throws IOException {
        McpStdioServer stdioServer = new McpStdioServer();
        stdioServer.initialize(mcpService);
        stdioServer.start();
    }

}
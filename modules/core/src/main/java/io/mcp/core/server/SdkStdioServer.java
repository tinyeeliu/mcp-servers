package io.mcp.core.server;

import java.io.IOException;

import io.mcp.core.protocol.McpService;

/**
 * Official SDK STDIO server. This is the original server that does not work in native image.
 */
public class SdkStdioServer {

    public void start(McpService mcpService) throws IOException {
        McpStdioServer stdioServer = new McpStdioServer();
        stdioServer.initialize(mcpService);
        stdioServer.start();
    }

}
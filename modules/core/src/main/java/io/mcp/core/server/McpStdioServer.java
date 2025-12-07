package io.mcp.core.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import io.mcp.core.protocol.McpService;
import io.mcp.core.utility.Utility;

/**
 * A custom STDIO server implementation that works in GraalVM native images.
 *
 * This replaces the official SDK server because the async transport provider
 * has threading issues in native images. Instead, it uses the same StreamableServer
 * approach as the HTTP server, adapted for stdio communication.
 */
public class McpStdioServer {

    private final StreamableServer mcpServer;

    public McpStdioServer() {
        this.mcpServer = new StreamableServer();
    }

    /**
     * Initialize the server with an MCP service.
     * This sets up the tools, prompts, and resources from the service.
     *
     * @param service The MCP service containing tools, prompts, and resources
     */
    public void initialize(McpService service) {
        mcpServer.initialize(service);
    }

    /**
     * Start the stdio server. This method will block and handle JSON-RPC messages
     * from stdin and send responses to stdout until stdin is closed.
     *
     * @throws IOException if there's an error reading from stdin or writing to stdout
     */
    public void start() throws IOException {
        Utility.debug("McpStdioServer starting...");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(System.out, true);

        String line;
        int messageCount = 0;

        while ((line = reader.readLine()) != null) {
            messageCount++;
            Utility.debug("Received message #" + messageCount + ": " + line);

            try {
                // Handle the JSON-RPC request using StreamableServer
                // Use a fixed session ID for stdio (single session)
                String sessionId = "stdio-session";
                String response = mcpServer.handleRequestSync(line, sessionId);

                if (response != null) {
                    // Write the response to stdout
                    writer.println(response);
                    writer.flush();
                    Utility.debug("Sent response #" + messageCount + ": " + response);
                } else {
                    // Null response means this was a notification, no response needed
                    Utility.debug("Notification processed, no response sent");
                }

            } catch (Exception e) {
                Utility.debug("Error processing message #" + messageCount + ": " + e.getMessage());

                // Send a generic error response
                String errorResponse = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Internal server error\"},\"id\":null}";
                writer.println(errorResponse);
                writer.flush();
                Utility.debug("Sent error response: " + errorResponse);
            }
        }

        Utility.debug("McpStdioServer finished processing " + messageCount + " messages");
    }

    /**
     * Get the content type used by this server.
     * @return The content type string
     */
    public String getContentType() {
        return mcpServer.getContentType();
    }
}

package io.mcp.core.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import io.mcp.core.utility.Utility;

/**
 * A very simple STDIO server for testing basic connectivity with JVM and GraalVM native.
 * This server just responds to basic JSON-RPC messages to verify stdio communication works.
 */
public class SimpleStdioTestServer {

    public static void main(String[] args) throws IOException {
        // Redirect stderr to log file to capture all error output
        Utility.redirectStdErrToLog();

        System.err.println("SimpleStdioTestServer starting...");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        String line;
        int messageCount = 0;

        while ((line = reader.readLine()) != null) {
            messageCount++;
            System.err.println("Received message #" + messageCount + ": " + line);

            // Handle basic JSON-RPC messages
            if (line.contains("\"method\":\"ping\"")) {
                // Respond to ping with pong
                String response = "{\"jsonrpc\":\"2.0\",\"result\":{},\"id\":" +
                    extractId(line) + "}";
                writer.println(response);
                System.err.println("Sent ping response: " + response);
            } else if (line.contains("\"method\":\"initialize\"")) {
                // Respond to initialize with basic server info
                String response = "{\"jsonrpc\":\"2.0\",\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"serverInfo\":{\"name\":\"SimpleStdioTestServer\",\"version\":\"1.0.0\"}},\"id\":" +
                    extractId(line) + "}";
                writer.println(response);
                System.err.println("Sent initialize response: " + response);
            } else {
                // Echo back unknown messages with an error for debugging
                String response = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Unknown method\"},\"id\":" +
                    extractId(line) + "}";
                writer.println(response);
                System.err.println("Sent error response for unknown method: " + response);
            }
        }

        System.err.println("SimpleStdioTestServer shutting down after " + messageCount + " messages");
    }

    private static String extractId(String json) {
        // Simple ID extraction - look for "id": followed by a number
        int idIndex = json.indexOf("\"id\":");
        if (idIndex != -1) {
            int start = idIndex + 5;
            int end = json.indexOf(',', start);
            if (end == -1) {
                end = json.indexOf('}', start);
            }
            if (end != -1) {
                return json.substring(start, end).trim();
            }
        }
        return "null";
    }
}

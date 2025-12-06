package io.mcp.core.server;

/**
 * Launcher for core module that can run different test servers.
 */
public class CoreLauncher {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: CoreLauncher <server_type> [transport]");
            System.err.println("Server types:");
            System.err.println("  simple-test - Simple STDIO test server");
            System.exit(1);
        }

        String serverType = args[0];
        String transport = args.length > 1 ? args[1] : "stdio";

        if ("simple-test".equals(serverType)) {
            if ("stdio".equals(transport)) {
                System.err.println("Starting SimpleStdioTestServer...");
                SimpleStdioTestServer.main(new String[0]);
            } else {
                System.err.println("Error: SimpleStdioTestServer only supports stdio transport");
                System.exit(1);
            }
        } else {
            System.err.println("Error: Unknown server type: " + serverType);
            System.exit(1);
        }
    }
}

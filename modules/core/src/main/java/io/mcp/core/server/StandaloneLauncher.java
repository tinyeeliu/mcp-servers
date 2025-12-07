package io.mcp.core.server;

import java.io.IOException;

import io.mcp.core.protocol.McpService;
import io.mcp.core.utility.Utility;


public class StandaloneLauncher {
    

    /*

    Command line arguments:
    - transport: stdio, http, sse
    - classPath: the path to the service class
    */

    public static void main(String[] args) throws Exception {
        // Redirect stderr to log file to capture all error output
        Utility.redirectStdErrToLog();


        String transport = args[0];
        String classPath = args[1];
        Class<?> serviceClass = Class.forName(classPath);
        McpService service = (McpService) serviceClass.getDeclaredConstructor().newInstance();
        launch(transport, service);
    }

    public static boolean DEBUG_SIMPLE_SERVER = true;

    public static void launch(String transport, McpService service) throws IOException {
       
    
        if (transport == null) {
            transport = "stdio";
        }

        if (transport.equals("stdio")) {

            if (DEBUG_SIMPLE_SERVER) {
                //SimpleStdioTestServer.main(new String[0]);
                //SimpleSdkStdioAsyncTestServer.main(new String[0]);
                SimpleSdkStdioSyncTestServer.main(new String[0]);
            } else {

                // Use the new native-image compatible stdio server
                McpStdioServer stdioServer = new McpStdioServer();
                stdioServer.initialize(service);
                stdioServer.start();
            }

        } else if (transport.equals("stdio-async")) {

            // Alternative: use the official SDK async server (problematic in native images)
            StdioServer stdioServer = new StdioServer();
            stdioServer.start(service);
        } else if (transport.equals("http")) {
            // Streamable HTTP transport
            StreamableServer mcpServer = new StreamableServer();
            mcpServer.initialize(service);
            
            McpHttpServer httpServer = new McpHttpServer(mcpServer);
            httpServer.startStreamableServer();
        } else if (transport.equals("sse")) {
            // SSE transport (legacy)
            StreamableServer mcpServer = new StreamableServer();
            mcpServer.initialize(service);
            
            McpHttpServer httpServer = new McpHttpServer(mcpServer);
            httpServer.startSseServer();
        } else if (transport.equals("http-all")) {
            // Both SSE and Streamable HTTP
            StreamableServer mcpServer = new StreamableServer();
            mcpServer.initialize(service);
            
            McpHttpServer httpServer = new McpHttpServer(mcpServer);
            httpServer.startServer();
        }
    }


}

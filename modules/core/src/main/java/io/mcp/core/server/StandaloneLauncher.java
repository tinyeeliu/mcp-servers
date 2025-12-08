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
  
        String transport = args[0];
        String classPath = args[1];

        Utility.setDebug(true);
        if(args.length > 2) {
            String debug = args[2];
            if("debug".equals(debug)) {
                Utility.redirectStdErrToLog();
            } 
        }

        Class<?> serviceClass = Class.forName(classPath);
        McpService service = (McpService) serviceClass.getDeclaredConstructor().newInstance();
        launch(transport, service);
    }

 
    public static void launch(String transport, McpService service) throws IOException {
       
         //update the version number whenever we make changes to the project
        Utility.debug("StandaloneLauncher v1 starting...");
        Utility.debug("transport: " + transport);

        if (transport == null) {
            transport = "http";
        }

        if (transport.equals("stdio")) {

            if (Utility.isNative()) {
                // Use the new native-image compatible stdio server
                McpStdioServer stdioServer = new McpStdioServer();
                stdioServer.initialize(service);
                stdioServer.start();
            } else {

                //Use the official SDK STDIO server
                SdkStdioServer stdioServer = new SdkStdioServer();
                stdioServer.start(service);
            }

        } else if (transport.equals("http")) {
            // Streamable HTTP transport - now supports multiple modules
            McpHttpServer httpServer = new McpHttpServer();
            httpServer.startStreamableServer();
        } else if (transport.equals("sse")) {
            // SSE transport (legacy) - now supports multiple modules
            McpHttpServer httpServer = new McpHttpServer();
            httpServer.startSseServer();
        } else if (transport.equals("http-all")) {
            // Both SSE and Streamable HTTP - now supports multiple modules
            McpHttpServer httpServer = new McpHttpServer();
            httpServer.startServer();
        }
    }


}

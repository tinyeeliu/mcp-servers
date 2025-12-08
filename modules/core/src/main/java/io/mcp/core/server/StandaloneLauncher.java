package io.mcp.core.server;

import java.io.IOException;
import java.util.List;

import io.mcp.core.protocol.McpService;
import io.mcp.core.utility.ServiceUtility;
import io.mcp.core.utility.Utility;


public class StandaloneLauncher {
    
    /*

    Command line arguments:
    - transport: stdio, http, sse
    - classPath: the path to the service class
    */

    public static void main(String[] args) throws Exception {
  
        String transport = "http";
        if(args.length > 0) {
            transport = args[0];
        }
        String moduleName = null;
        
        if(args.length > 1) {
            moduleName = args[1];
        }

        Utility.setDebug(true);
       
        List<McpService> services = ServiceUtility.getRegisteredServices();

        Utility.debug("Services founded: " + services.size());
        for(McpService service : services) {
            Utility.debug("Service: " + service.getModule() + " - " + service.getClass().getName());
        }


        if(services.size() == 1) {
            launch(transport, services.get(0));
            return;
        }else if(moduleName != null){ 
            Utility.debug("Launching module: " + moduleName);
            for (McpService service : services) {
                if(service.getModule().equals(moduleName)) {
                    launch(transport, service);
                    return;
                }
            }
        }else if(services.size() > 1) {
            if("stdio".equals(transport)) {
                throw new IllegalArgumentException("stdio transport is not supported for multiple modules");
            }
            launch(transport, null);
        }

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

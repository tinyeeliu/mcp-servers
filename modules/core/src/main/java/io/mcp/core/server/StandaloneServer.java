package io.mcp.core.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import io.mcp.core.protocol.McpService;


public class StandaloneServer {
    

    /*

    Command line arguments:
    - transport: stdio, http.
    - classPath: the path to the service class
    */

    public static void main(String[] args) throws Exception {

    
        String transport = args[0];
        String classPath = args[1];
        Class<?> serviceClass = Class.forName(classPath);
        McpService service = (McpService) serviceClass.getDeclaredConstructor().newInstance();
        launch(transport, service);
    }


    public static void launch(String transport, McpService service) throws IOException {
        if (transport.equals("stdio")) {
            StdioServer stdioServer = new StdioServer();
            stdioServer.start(service);
        } else if (transport.equals("http")) {
            StreamableServer mcpServer = new StreamableServer();
            mcpServer.initialize(service);
            
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            httpServer.createContext("/mcp", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    String requestBody = new String(exchange.getRequestBody().readAllBytes());
                    String response = mcpServer.handleRequestSync(requestBody, null);
                    
                    exchange.getResponseHeaders().set("Content-Type", mcpServer.getContentType());
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                }
            });
            httpServer.start();
            System.out.println("MCP Server running on http://localhost:8080/mcp");
        }
    }


}

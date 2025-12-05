package io.mcp.core.server;

/*

Pure java implementation of MCP HTTP server.

Support both SSE and Streamable HTTP transport.


*/

public class McpHttpServer {
    
   private StreamableServer mcpServer;

   public McpHttpServer(StreamableServer mcpServer) {
    this.mcpServer = mcpServer;
   }

   public void startSseServer(){

   }

   public void startStreamableServer(){

   }
   

}

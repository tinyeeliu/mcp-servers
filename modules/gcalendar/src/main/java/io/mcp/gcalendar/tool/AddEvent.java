package io.mcp.gcalendar.tool;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import io.mcp.core.base.BaseMcpTool;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

public class AddEvent extends BaseMcpTool {

 
    @Override
    public String getName() {
        return "addEvent";
    }

    @Override
    public String getModule() {
        return "gcalendar";
    }


    @Override
    public CompletableFuture<McpSchema.CallToolResult> call(McpAsyncServerExchange exchange, CallToolRequest request) {

        //TODO: Implement the logic to add an event to Google Calendar
      
        return CompletableFuture.completedFuture(
            McpSchema.CallToolResult.builder()
                .addTextContent("Added event successfully")
                .isError(false)
                .build()
        );
    }

 


}

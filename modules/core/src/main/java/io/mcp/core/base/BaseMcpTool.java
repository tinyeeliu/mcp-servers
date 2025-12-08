package io.mcp.core.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.JsonSchemaUtility;
import io.mcp.core.utility.Utility;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import reactor.core.publisher.Mono;

public abstract class BaseMcpTool implements McpTool {
    @Override
    public McpServerFeatures.AsyncToolSpecification getToolSpecification() {

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(getTool())
                .callHandler((exchange, request) -> {
                    try {
                        return Mono.fromFuture(call(exchange, request));
                    } catch (Exception e) {
                        return Mono.just(
                            McpSchema.CallToolResult.builder()
                                .addTextContent(e.getMessage())
                                .isError(true)
                                .build()
                        );
                    }
                })
                .build();
    }

    public CompletableFuture<CallToolResult> callWithLog(McpAsyncServerExchange exchange, CallToolRequest request){

        if(Utility.isDebug()){

            debugLog(exchange, request);
            return call(exchange, request).thenApply(result -> {
                debugLog(result);
                return result;
            });
        }else{
            return call(exchange, request);
        
        }
    }
    public void debugLog(CallToolResult result){
        StringBuilder sb = new StringBuilder();
        sb.append("TOOL RESPONSE [").append(getModule()).append(".").append(getName()).append("] ");

        // Add error status
        if (result.isError() != null && result.isError()) {
            sb.append("ERROR: ");
        } else {
            sb.append("SUCCESS: ");
        }

        // Add content summary
        if (result.content() != null) {
            sb.append(result.content().size()).append(" content items");
            for (int i = 0; i < Math.min(result.content().size(), 3); i++) {
                var content = result.content().get(i);
                if (content instanceof McpSchema.TextContent textContent) {
                    String text = textContent.text();
                    if (text != null) {
                        // Truncate long text for readability
                        String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                        sb.append(" [").append(preview.replaceAll("\\s+", " ")).append("]");
                    }
                } else {
                    sb.append(" [").append(content.getClass().getSimpleName()).append("]");
                }
            }
            if (result.content().size() > 3) {
                sb.append(" ...");
            }
        } else {
            sb.append("no content");
        }

        Utility.debug(sb.toString());
    }

    public void debugLog(McpAsyncServerExchange exchange, CallToolRequest request){
        StringBuilder sb = new StringBuilder();
        sb.append("TOOL REQUEST [").append(getModule()).append(".").append(getName()).append("] ");

        // Add arguments
        if (request.arguments() != null && !request.arguments().isEmpty()) {
            sb.append("args: ");
            boolean first = true;
            for (var entry : request.arguments().entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        } else {
            sb.append("no args");
        }

        // Add metadata if available
        if (request.meta() != null && !request.meta().isEmpty()) {
            sb.append(" | meta: ");
            boolean first = true;
            for (var entry : request.meta().entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }

        

        // Add client info if available
        if (exchange.getClientInfo() != null) {
            sb.append(" | client: ").append(exchange.getClientInfo().name());
            if (exchange.getClientInfo().version() != null) {
                sb.append(" v").append(exchange.getClientInfo().version());
            }
        }

        // Add session info from transport context if available
        if (exchange.transportContext() != null) {
            Object sessionId = exchange.transportContext().get("session-id");
            if (sessionId != null) {
                sb.append(" | session: ").append(sessionId);
            }
        }

        Utility.debug(sb.toString());
    }

    @Override
    public McpSchema.Tool getTool() {

        try {
            JsonNode jsonNode = loadJsonSchema("tool");
            return JsonSchemaUtility.getTool(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tool specification", e);
        }


    }


    @Override
    public List<McpServerFeatures.AsyncPromptSpecification> getPromptSpecifications() {

        try {
            JsonNode jsonNode = loadJsonSchema("prompt");
            if (jsonNode == null) {
                return new ArrayList<>();
            }
            return JsonSchemaUtility.getPrompts(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt specification", e);
        }


    }

    @Override
    public List<McpServerFeatures.AsyncResourceSpecification> getResourceSpecifications() {
        try {
            JsonNode jsonNode = loadJsonSchema("resource");
            if (jsonNode == null) {
                return new ArrayList<>();
            }
            return JsonSchemaUtility.getResources(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource specification", e);
        }
    }

    private JsonNode loadJsonSchema(String type) throws IOException {
    
        String path = "io/mcp/spec/"+ getModule() + "/"+ type + "/" + getName() + ".json";
        String jsonSchema = JsonSchemaUtility.loadJsonSchema(path);
        if (jsonSchema == null) {
            Utility.debug("No " + type + " schema found for " + path);
            return null;
        }
        JsonNode jsonNode = JsonSchemaUtility.toJsonNode(jsonSchema);
        return jsonNode;
       
    }

    @Override
    public List<McpServerFeatures.AsyncResourceTemplateSpecification> getResourceTemplateSpecifications() {
        try {
         
            JsonNode jsonNode = loadJsonSchema("template");
            if (jsonNode == null) {
                return new ArrayList<>();
            }
            return JsonSchemaUtility.getTemplates(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource template specification", e);
        }
    }
}

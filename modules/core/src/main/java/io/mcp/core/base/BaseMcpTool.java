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
        Utility.debug("XXX");
        
    }

    public void debugLog(McpAsyncServerExchange exchange, CallToolRequest request){
        Utility.debug("XXX");
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

package io.mcp.core.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.mcp.core.protocol.McpTool;
import io.mcp.core.utility.JsonSchemaUtility;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
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
    
        String jsonSchema = JsonSchemaUtility.loadJsonSchema("io/mcp/spec/"+ getModule() + "/"+ type + "/" + getName() + ".json");
        if (jsonSchema == null) {
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

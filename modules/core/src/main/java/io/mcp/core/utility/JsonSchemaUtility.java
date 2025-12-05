package io.mcp.core.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

public class JsonSchemaUtility {
 
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 


    public static String loadJsonSchema(String path) throws IOException {

        //load resource from classpath - ensure path starts with "/"
        String resourcePath = path.startsWith("/") ? path : "/" + path;
        InputStream inputStream = JsonSchemaUtility.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        return new String(inputStream.readAllBytes());
    }

    public static JsonNode toJsonNode(String jsonString) throws IOException {
        return OBJECT_MAPPER.readTree(jsonString);
    }

    //given a json node, return a McpSchema.JsonSchema
    private static McpSchema.JsonSchema toJsonSchema(JsonNode jsonNode) throws IOException {
        String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;
        
        Map<String, Object> properties = null;
        if (jsonNode.has("properties")) {
            properties = OBJECT_MAPPER.convertValue(jsonNode.get("properties"), 
                new TypeReference<Map<String, Object>>() {});
        }
        
        List<String> required = null;
        if (jsonNode.has("required")) {
            required = OBJECT_MAPPER.convertValue(jsonNode.get("required"), 
                new TypeReference<List<String>>() {});
        }
        
        return new McpSchema.JsonSchema(
            type,
            properties,
            required,
            null,
            null,
            null
        );
    }

    //given a json node, return a McpSchema.Tool
    public static McpSchema.Tool getTool(JsonNode jsonNode) throws IOException {
        String name = jsonNode.get("name").asText();
        String description = jsonNode.get("description").asText();
        McpSchema.JsonSchema inputSchema = toJsonSchema(jsonNode.get("inputSchema"));

        return McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }


    public static List<McpServerFeatures.AsyncPromptSpecification> getPrompts(JsonNode jsonNode) throws IOException {
        List<McpServerFeatures.AsyncPromptSpecification> promptSpecifications = new ArrayList<>();

        if (!jsonNode.isArray()) {
            throw new IOException("Prompts JSON must be an array");
        }

        for (JsonNode promptNode : jsonNode) {
            String name = promptNode.get("name").asText();
            String title = promptNode.get("title").asText();
            String description = promptNode.get("description").asText();

            // Parse arguments
            List<McpSchema.PromptArgument> arguments = new ArrayList<>();
            if (promptNode.has("arguments") && promptNode.get("arguments").isArray()) {
                for (JsonNode argNode : promptNode.get("arguments")) {
                    String argName = argNode.get("name").asText();
                    String argDescription = argNode.get("description").asText();
                    boolean required = argNode.get("required").asBoolean(false);

                    arguments.add(new McpSchema.PromptArgument(argName, argDescription, required));
                }
            }

            // Create the Prompt
            McpSchema.Prompt prompt = new McpSchema.Prompt(name, description, arguments);

            // Create the AsyncPromptSpecification with a handler
            // Note: This is a basic handler that returns a simple message.
            // In a real implementation, the handler logic should be provided by the specific tool/service.
            McpServerFeatures.AsyncPromptSpecification spec = new McpServerFeatures.AsyncPromptSpecification(
                prompt,
                (exchange, request) -> {
                    // Basic prompt handler - returns the prompt description and arguments
                    StringBuilder content = new StringBuilder();
                    content.append("Prompt: ").append(title).append("\n");
                    content.append("Description: ").append(description).append("\n");

                    if (!arguments.isEmpty()) {
                        content.append("Arguments:\n");
                        for (McpSchema.PromptArgument arg : arguments) {
                            content.append("- ").append(arg.name()).append(": ").append(arg.description());
                            if (arg.required()) {
                                content.append(" (required)");
                            }
                            content.append("\n");
                        }
                    }

                    return Mono.just(
                        new McpSchema.GetPromptResult(
                            description,
                            List.of(new McpSchema.PromptMessage(
                                McpSchema.Role.USER,
                                new McpSchema.TextContent(content.toString())
                            ))
                        )
                    );
                }
            );

            promptSpecifications.add(spec);
        }

        return promptSpecifications;
    }
}

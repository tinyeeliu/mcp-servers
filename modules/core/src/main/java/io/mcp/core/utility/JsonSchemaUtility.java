package io.mcp.core.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.spec.McpSchema;

public class JsonSchemaUtility {
 
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
 


    public static String loadJsonSchema(String path) throws IOException {

        //load resource from classpath
        InputStream inputStream = JsonSchemaUtility.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new RuntimeException("Resource not found: " + path);
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
}

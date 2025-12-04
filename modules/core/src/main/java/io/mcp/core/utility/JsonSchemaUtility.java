package io.mcp.core.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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

    public static McpSchema.JsonSchema toJsonSchema(JsonNode jsonNode) throws IOException {
        String type = jsonNode.get("type").asText();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = jsonNode.has("properties") ?
            (Map<String, Object>) OBJECT_MAPPER.convertValue(jsonNode.get("properties"), Map.class) : null;

        @SuppressWarnings("unchecked")
        List<String> required = jsonNode.has("required") ?
            (List<String>) OBJECT_MAPPER.convertValue(jsonNode.get("required"), List.class) : null;

        // For now, set optional fields to null like in GenerateRandom example
        Object additionalProperties = null;
        Object items = null;
        Object enumValues = null;

        return new McpSchema.JsonSchema(type, properties, required, additionalProperties, items, enumValues);
    }

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

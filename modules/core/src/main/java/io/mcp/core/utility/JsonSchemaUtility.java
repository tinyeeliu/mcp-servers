package io.mcp.core.utility;

import java.io.IOException;
import java.io.InputStream;

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
    }

    public static McpSchema.Tool getTool(JsonNode jsonNode) {

    }
}

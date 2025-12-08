package io.mcp.core.utility;

public class ConfigUtility {
    

    public static String getString(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key);   
        }
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

}

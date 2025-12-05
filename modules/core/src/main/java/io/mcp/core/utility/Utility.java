package io.mcp.core.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utility {

    private static boolean DEBUG = true;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    /**
     * Log debug messages to stderr.
     * Uses stderr to avoid interfering with stdout JSON-RPC communication.
     * 
     * @param messages Objects to log, will be concatenated with spaces
     */
    public static void debug(Object... messages) {
        if (!DEBUG) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[DEBUG ");
        sb.append(TIMESTAMP_FORMAT.format(LocalDateTime.now()));
        sb.append("] ");
        
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(messages[i] != null ? messages[i].toString() : "null");
        }
        
        System.err.println(sb.toString());
    }
}
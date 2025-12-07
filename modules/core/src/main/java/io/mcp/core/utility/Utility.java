package io.mcp.core.utility;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utility {

    private static boolean DEBUG = true;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String ERROR_LOG_FILE = "mcp_server_error.log";
    private static PrintWriter errorLogWriter;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    /**
     * Log debug messages to error log file.
     * Uses file logging to avoid interfering with stdout JSON-RPC communication.
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

        writeToErrorLog(sb.toString());
    }

    /**
     * Write message to error log file
     * @param message The message to write
     */
    private static synchronized void writeToErrorLog(String message) {
        try {
            if (errorLogWriter == null) {
                errorLogWriter = new PrintWriter(new FileWriter(ERROR_LOG_FILE, true));
            }
            errorLogWriter.println(message);
            errorLogWriter.flush();
        } catch (IOException e) {
            // Fallback to stderr if file logging fails
            System.err.println("Failed to write to error log: " + e.getMessage());
            System.err.println(message);
        }
    }
}
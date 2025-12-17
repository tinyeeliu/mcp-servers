package io.mcp.core.utility;

import io.mcp.core.protocol.McpContainer;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class Utility {

    private static final int DEFAULT_PORT = 8080;
    private static boolean DEBUG = true;
    private static boolean FILE_LOGGING = false;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String ERROR_LOG_FILE = "mcp_server_error.log";
    private static PrintWriter errorLogWriter;
    private static boolean stderrRedirected = false;
    private static McpContainer mcpContainer = null;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static boolean isErrorRedirected() {
        return stderrRedirected;
    }

    public static void setFileLogging(boolean fileLogging) {
        FILE_LOGGING = fileLogging;
    }

    public static boolean isFileLogging() {
        return FILE_LOGGING;
    }


    public static void setMcpContainer(McpContainer container){
        mcpContainer = container;
    }

    public static McpContainer getMcpContainer() {
        return mcpContainer;
    }

    public static boolean isNative(){

		return "Substrate VM".equals(System.getProperty("java.vm.name"));

	}

    /**
     * Redirect System.err to write to the error log file
     * This captures all stderr output, not just debug messages
     */
    public static void redirectStdErrToLog() {

        if(!FILE_LOGGING){
            return;
        }

        if (stderrRedirected) {
            return; // Already redirected
        }

        System.err.println("Redirecting stderr to log file: " + ERROR_LOG_FILE);

        try {
            PrintStream errStream = new PrintStream(new FileOutputStream(ERROR_LOG_FILE, true), true);
            System.setErr(errStream);
            stderrRedirected = true;
        } catch (IOException e) {
            System.err.println("Failed to redirect stderr to log file: " + e.getMessage());
        }
    }


    public static void debug(Throwable e) {
        if(mcpContainer != null){
            mcpContainer.log("ERROR", e);
            return;
        }
        e.printStackTrace(System.err);
    }

    public static void debug(String message, Throwable e) {

        if(mcpContainer != null){
            mcpContainer.log(message, e);
            return;
        }


        System.err.println(message);
        e.printStackTrace(System.err);
    }

    /**
     * Log debug messages to error log file.
     * Uses file logging to avoid interfering with stdout JSON-RPC communication.
     * If stderr is redirected to the log file, this will use stderr to avoid double logging.
     *
     * @param messages Objects to log, will be concatenated with spaces
     */
    public static void debug(Object... messages) {

        if(mcpContainer != null){
            mcpContainer.log(System.Logger.Level.DEBUG, messages);
            return;
        }

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

        String message = sb.toString();


        // If stderr is redirected to the log file, use stderr to avoid double logging
        if (FILE_LOGGING && stderrRedirected) {
            writeToErrorLog(message);
        } else {
            System.err.println(message);
        }
    }

    public static int getConfiguredPort() {
        String portProperty = System.getProperty("http.port");
        if (portProperty != null) {
            try {
                return Integer.parseInt(portProperty);
            } catch (NumberFormatException e) {
                System.err.println("Invalid http.port system property: " + portProperty + ", using default port " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
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
            System.err.println("Failed to write to error log 3: " + e.getMessage());
            System.err.println(message);
        }
    }
}

package io.mcp.core.protocol;

public interface McpContainer {

    /*
    Send the log to container to be processed.

     */
    public void log(System.Logger.Level level, Object... msgs);
    public void log(String message, Throwable e);

}

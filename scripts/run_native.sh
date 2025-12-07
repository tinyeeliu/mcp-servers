#!/bin/bash

# Give a transport and run the native MCP project with MCP Inspector.

if [ $# -gt 1 ]; then
    echo "Usage: $0 [transport]"
    echo ""
    echo "Examples:"
    echo "  $0 stdio       - Run with stdio transport (default)"
    echo "  $0 http        - Run with Streamable HTTP transport"
    echo "  $0 sse         - Run with SSE transport"
    echo "  $0 http-all    - Run with both SSE and Streamable HTTP"
    echo ""
    echo "Transports:"
    echo "  stdio     - Standard I/O transport (default)"
    echo "  http      - Streamable HTTP transport (POST /mcp)"
    echo "  sse       - SSE transport (GET /sse + POST /messages)"
    echo "  http-all  - Both SSE and Streamable HTTP"
    echo ""
    echo "Runs the native executable with MCP Inspector."
    exit 1
fi

TRANSPORT=${1:-stdio}  # Default to stdio if not specified
EXE_FILE="projects/mcp/target/mcp-service"

# Kill any existing processes on inspector ports and MCP server port
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:8080 2>/dev/null) 2>/dev/null || true

if [ ! -f "$EXE_FILE" ]; then
    echo "Error: Native executable not found: $EXE_FILE"
    echo "Please build the native image first using: ./scripts/build_native.sh"
    exit 1
fi

echo "Running MCP Inspector for native MCP project"
echo "Transport: $TRANSPORT"
echo "Executable: $EXE_FILE"
echo ""

cd "$(dirname "$0")/.." || exit 1

cleanup() {
    if [ -n "$SERVER_PID" ]; then
        echo "Stopping server (PID: $SERVER_PID)..."
        kill $SERVER_PID 2>/dev/null || true
    fi
    if [ -n "$INSPECTOR_PID" ]; then
        echo "Stopping inspector (PID: $INSPECTOR_PID)..."
        kill $INSPECTOR_PID 2>/dev/null || true
    fi
}
trap cleanup EXIT

case "$TRANSPORT" in
    stdio)
        # Create the error log file first
        touch mcp_server_error.log
        npx @modelcontextprotocol/inspector@0.16.7 --transport stdio "$EXE_FILE" stdio &
        INSPECTOR_PID=$!
        echo "Started inspector in background (PID: $INSPECTOR_PID)"
        echo "Tailing error log..."
        tail -f mcp_server_error.log
        ;;
    http)
        # Create the error log file first
        touch mcp_server_error.log
        # Start the server in background with Streamable HTTP transport
        "$EXE_FILE" http &
        SERVER_PID=$!
        echo "Started Streamable HTTP server (PID: $SERVER_PID)"
        sleep 2

        # Run inspector with streamable HTTP transport in background
        echo "Connecting inspector to http://localhost:8080/mcp"
        npx @modelcontextprotocol/inspector@0.16.7 --transport streamable-http --url http://localhost:8080/mcp &
        INSPECTOR_PID=$!
        echo "Started inspector in background (PID: $INSPECTOR_PID)"
        echo "Tailing error log..."
        tail -f mcp_server_error.log
        ;;
    sse)
        # Create the error log file first
        touch mcp_server_error.log
        # Start the server in background with SSE transport
        "$EXE_FILE" sse &
        SERVER_PID=$!
        echo "Started SSE server (PID: $SERVER_PID)"
        sleep 2

        # Run inspector with SSE transport in background
        echo "Connecting inspector to http://localhost:8080/sse"
        npx @modelcontextprotocol/inspector@0.16.7 --transport sse --url http://localhost:8080/sse &
        INSPECTOR_PID=$!
        echo "Started inspector in background (PID: $INSPECTOR_PID)"
        echo "Tailing error log..."
        tail -f mcp_server_error.log
        ;;
    http-all)
        # Create the error log file first
        touch mcp_server_error.log
        # Start the server in background with both transports
        "$EXE_FILE" http-all &
        SERVER_PID=$!
        echo "Started combined HTTP server (PID: $SERVER_PID)"
        echo "  Streamable HTTP: http://localhost:8080/mcp"
        echo "  SSE endpoint:    http://localhost:8080/sse"
        sleep 2

        # Default to Streamable HTTP for inspector
        echo ""
        echo "Connecting inspector to Streamable HTTP endpoint..."
        npx @modelcontextprotocol/inspector@0.16.7 --transport streamable-http --url http://localhost:8080/mcp &
        INSPECTOR_PID=$!
        echo "Started inspector in background (PID: $INSPECTOR_PID)"
        echo "Tailing error log..."
        tail -f mcp_server_error.log
        ;;
    *)
        echo "Error: Unsupported transport '$TRANSPORT'."
        echo "Use one of: stdio, http, sse, http-all"
        exit 1
        ;;
esac

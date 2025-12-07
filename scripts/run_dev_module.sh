#!/bin/bash

# Give a module name, run the server in stdio mode.

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name>"
    echo "Example: $0 random"
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi


./scripts/build_module.sh

# Kill any existing processes on inspector ports and MCP server port
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:8080 2>/dev/null) 2>/dev/null || true

echo "Running MCP server: $MODULE_NAME"
echo "JAR file: $JAR_FILE"
echo "Server will listen on stdin/stdout for MCP protocol messages..."
echo ""

cd "$(dirname "$0")/.." || exit 1
java -jar "$JAR_FILE"
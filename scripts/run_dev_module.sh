#!/bin/bash

# Give a module name, run the server in stdio mode.

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name>"
    echo "Example: $0 random"
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

./scripts/build_module.sh "$MODULE_NAME"

# Kill any existing processes on inspector ports and MCP server port
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:8080 2>/dev/null) 2>/dev/null || true

echo "Running MCP server: $MODULE_NAME"
echo "Server will listen on stdin/stdout for MCP protocol messages..."
echo ""

cd "$(dirname "$0")/.." || exit 1
mvn exec:java -pl "$MODULE_PATH" -q
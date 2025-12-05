#!/bin/bash

# Give a module name, run the server with MCP Inspector.

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name>"
    echo "Example: $0 random"
    echo ""
    echo "Builds and runs the packaged JAR with MCP Inspector."
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

# Kill any existing processes on inspector ports
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true

cd "$(dirname "$0")/.." || exit 1

# Build and run packaged JAR
./scripts/build_module.sh $MODULE_NAME

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please build the module first using: ./scripts/build_module.sh $MODULE_NAME"
    exit 1
fi

echo "Running MCP Inspector for module: $MODULE_NAME"
echo "JAR file: $JAR_FILE"
echo "Server command: java -jar $JAR_FILE"
echo ""

npx @modelcontextprotocol/inspector@0.17.2 --transport stdio java --enable-preview -jar "$JAR_FILE" stdio 2>&1 | cat


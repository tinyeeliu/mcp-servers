#!/bin/bash

# Give a module name, run the server with MCP Inspector.

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

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please build the module first using: ./scripts/build_module.sh $MODULE_NAME"
    exit 1
fi

echo "Running MCP Inspector for module: $MODULE_NAME"
echo "JAR file: $JAR_FILE"
echo "Server command: java -jar $JAR_FILE"
echo ""

kill -9 $(lsof -ti:6274)
kill -9 $(lsof -ti:6277)

# Run MCP Inspector with the stdio server
npx @modelcontextprotocol/inspector@0.17.2 --transport stdio java -jar "$JAR_FILE"


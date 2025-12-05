#!/bin/bash

# Run MCP server with specified transport.
# Argument 1: transport (stdio, http)
# Argument 2: module name

if [ $# -lt 2 ]; then
    echo "Usage: $0 <transport> <module_name>"
    echo "Example: $0 stdio random"
    echo ""
    echo "Transports: stdio, http"
    exit 1
fi

TRANSPORT=$1
MODULE_NAME=$2
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

cd "$(dirname "$0")/.." || exit 1

# Build and run packaged JAR
./scripts/build_module.sh $MODULE_NAME

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please build the module first using: ./scripts/build_module.sh $MODULE_NAME"
    exit 1
fi

echo "Running MCP server for module: $MODULE_NAME"
echo "Transport: $TRANSPORT"
echo "JAR file: $JAR_FILE"
echo ""

java -jar "$JAR_FILE" "$TRANSPORT"
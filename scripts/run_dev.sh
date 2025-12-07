#!/bin/bash

# Give a module name, run the server in stdio mode.

./scripts/build_module.sh

echo "Running MCP server: $MODULE_NAME"
echo "JAR file: $JAR_FILE"
echo "Server will listen on stdin/stdout for MCP protocol messages..."
echo ""

cd "$(dirname "$0")/.." || exit 1
java -jar "$JAR_FILE"
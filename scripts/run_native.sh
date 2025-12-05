#!/bin/bash

# Run native MCP server executable.
# Argument 1: transport (stdio, http, sse, http-all)
# Argument 2: module name

if [ $# -lt 2 ]; then
    echo "Usage: $0 <transport> <module_name>"
    echo "Example: $0 stdio random"
    echo ""
    echo "Transports:"
    echo "  stdio    - Standard input/output (for MCP protocol)"
    echo "  http     - HTTP transport (Streamable)"
    echo "  sse      - Server-Sent Events transport (legacy)"
    echo "  http-all - Both SSE and Streamable HTTP"
    exit 1
fi

TRANSPORT=$1
MODULE_NAME=$2
MODULE_PATH="modules/${MODULE_NAME}"
EXE_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

if [ ! -f "$EXE_FILE" ]; then
    echo "Error: Native executable not found: $EXE_FILE"
    echo "Please build the native image first using: ./scripts/build_native.sh $MODULE_NAME"
    exit 1
fi

echo "Running native MCP server for module: $MODULE_NAME"
echo "Transport: $TRANSPORT"
echo "Executable: $EXE_FILE"
echo ""

cd "$(dirname "$0")/.." || exit 1
"$EXE_FILE" "$TRANSPORT"

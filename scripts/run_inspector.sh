#!/bin/bash

# Give a module name, run the server with MCP Inspector.
# Supports hot reload via Maven exec plugin - just recompile and reconnect.

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name> [--jar]"
    echo "Example: $0 random"
    echo ""
    echo "Options:"
    echo "  --jar    Use JAR mode (builds and runs packaged JAR)"
    echo "  (default) Use Maven exec mode (supports hot reload - run 'mvn compile' to recompile)"
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"
USE_JAR=false

if [ "$2" = "--jar" ]; then
    USE_JAR=true
fi

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

# Kill any existing processes on inspector ports
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true

cd "$(dirname "$0")/.." || exit 1

if [ "$USE_JAR" = true ]; then
    # JAR mode - build and run packaged JAR
    ./scripts/build_module.sh $MODULE_NAME
    
    if [ ! -f "$JAR_FILE" ]; then
        echo "Error: JAR file not found: $JAR_FILE"
        echo "Please build the module first using: ./scripts/build_module.sh $MODULE_NAME"
        exit 1
    fi
    
    echo "Running MCP Inspector for module: $MODULE_NAME (JAR mode)"
    echo "JAR file: $JAR_FILE"
    echo "Server command: java -jar $JAR_FILE"
    echo ""
    
    npx @modelcontextprotocol/inspector@0.17.2 --transport stdio java --enable-preview -jar "$JAR_FILE"
else
    # Maven exec mode - supports hot reload
    echo "Running MCP Inspector for module: $MODULE_NAME (Maven exec mode)"
    echo ""
    echo "=== HOT RELOAD INSTRUCTIONS ==="
    echo "1. Make changes to your Java files"
    echo "2. Run in another terminal: mvn compile -pl $MODULE_PATH -q"
    echo "3. In the Inspector UI, click 'Disconnect' then 'Connect' to reconnect"
    echo "4. Your changes will be picked up!"
    echo "==============================="
    echo ""
    
    # Initial compile
    echo "Compiling module..."
    mvn compile -pl "$MODULE_PATH" -am -q
    
    if [ $? -ne 0 ]; then
        echo "Error: Initial compilation failed"
        exit 1
    fi
    
    echo "Starting MCP Inspector..."
    echo ""
    
    # Run MCP Inspector with Maven exec
    npx @modelcontextprotocol/inspector@0.17.2 --transport stdio mvn exec:java -pl "$MODULE_PATH" -q
fi


#!/bin/bash

# Run the MCP project in stdio mode using Maven.

echo "Running MCP project"
echo "Server will listen on stdin/stdout for MCP protocol messages..."
echo ""

cd "$(dirname "$0")/.." || exit 1
mvn exec:java -pl projects/mcp -Dexec.mainClass="io.mcp.random.RandomNumberServer"
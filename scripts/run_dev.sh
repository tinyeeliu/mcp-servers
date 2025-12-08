#!/bin/bash

# Run the MCP project in stdio mode using Maven.

echo "Running MCP project"
echo "Server will listen on http for MCP protocol messages..."
echo ""

./scripts/build_module.sh --clean
# Kill any existing processes on inspector ports and MCP server port
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:8080 2>/dev/null) 2>/dev/null || true

cd "$(dirname "$0")/../projects/mcp" || exit 1

# Run the MCP project in stdio mode using Maven.
#mvn clean exec:java -DskipTests -Dexec.mainClass="io.mcp.random.RandomNumberServer" 

rm target/mcp-service-1.0.0.jar

# Build the uber jar
mvn clean package -DskipTests

# Run the uber jar
echo "Starting MCP server from uber jar..."
java -jar target/mcp-service-1.0.0.jar

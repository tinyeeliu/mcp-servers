#!/bin/bash

# Run the MCP project in stdio mode using Maven.

echo "Running MCP project"
echo "Server will listen on http for MCP protocol messages..."
echo ""

# Build the project
./scripts/build.sh

# Change to the main project directory to run the jar
cd "$(dirname "$0")/../projects/mcp" || exit 1

# Run the MCP project in stdio mode using Maven.
#mvn clean exec:java -DskipTests -Dexec.mainClass="io.mcp.random.RandomNumberServer"

# Kill any existing processes on inspector ports and MCP server port
kill -9 $(lsof -ti:6274 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:6277 2>/dev/null) 2>/dev/null || true
kill -9 $(lsof -ti:8080 2>/dev/null) 2>/dev/null || true

# Run the uber jar
echo "Starting MCP server from uber jar..."
java -jar target/mcp-service-1.0.0.jar

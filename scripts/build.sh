#!/bin/bash

# Build the MCP service project with clean to ensure code is updated.
# This script builds all modules first, then builds the main mcp-service project.

echo "Building MCP service project..."
echo "Step 1: Building modules..."

# Build all modules with clean
./scripts/build_module.sh --clean

echo ""
echo "Step 2: Building main mcp-service project..."

# Change to the main project directory
cd "$(dirname "$0")/../projects/mcp" || exit 1

# Clean and package the main project
mvn clean package -DskipTests

echo ""
echo "Build completed successfully!"
echo "JAR file created: target/mcp-service-1.0.0.jar"


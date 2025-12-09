#!/bin/bash

# Build native image for MCP project using GraalVM.
# This builds the uber jar first, then creates a native image.

echo "Building native image for MCP project..."
echo "Step 1: Building uber jar..."
cd projects/mcp || exit 1
mvn clean package -DskipTests

# Check if jar build succeeded
if [ $? -ne 0 ]; then
    echo "JAR build failed. Exiting..."
    exit 1
fi

echo "Step 2: Building native image with GraalVM..."
mvn package -DskipTests -Pnative

# Check if native build succeeded
if [ $? -ne 0 ]; then
    echo "Native image build failed. Exiting..."
    exit 1
fi
#!/bin/bash

# Build native image for MCP project using GraalVM.
# This builds the uber jar first, then creates a native image.

echo "Building native image for MCP project..."
echo "Step 1: Building uber jar..."
cd projects/mcp || exit 1
mvn clean package -DskipTests
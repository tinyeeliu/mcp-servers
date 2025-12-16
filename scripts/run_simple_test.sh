#!/bin/bash

# Run the simple STDIO test server for testing basic connectivity.

if [ $# -lt 1 ]; then
    echo "Usage: $0 <mode>"
    echo ""
    echo "Modes:"
    echo "  jvm     - Run with JVM"
    echo "  native  - Run with GraalVM native build"
    echo ""
    exit 1
fi

MODE=$1
MODULE_NAME="core"
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"
NATIVE_IMAGE="$MODULE_PATH/target/mcp-core"

cd "$(dirname "$0")/.." || exit 1

case "$MODE" in
    jvm)
        echo "Building and running simple test server with JVM..."
        ./scripts/build_module.sh $MODULE_NAME

        # Check if build succeeded
        if [ $? -ne 0 ]; then
            echo "Build failed. Exiting..."
            exit 1
        fi

        if [ ! -f "$JAR_FILE" ]; then
            echo "Error: JAR file not found: $JAR_FILE"
            exit 1
        fi

        echo "Running JVM version..."
        java -jar "$JAR_FILE" simple-test stdio
        ;;
    native)
        echo "Building and running simple test server with GraalVM native..."

        # Build native image
        ./scripts/build_native.sh $MODULE_NAME

        # Check if build succeeded
        if [ $? -ne 0 ]; then
            echo "Build failed. Exiting..."
            exit 1
        fi

        if [ ! -f "$NATIVE_IMAGE" ]; then
            echo "Error: Native image not found: $NATIVE_IMAGE"
            exit 1
        fi

        echo "Running native version..."
        "$NATIVE_IMAGE" simple-test stdio
        ;;
    *)
        echo "Error: Unknown mode '$MODE'. Use 'jvm' or 'native'"
        exit 1
        ;;
esac

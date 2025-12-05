#!/bin/bash

# Give a module name, run the unit tests cases

if [ $# -eq 0 ]; then
    echo "Usage: $0 <module_name>"
    echo "Example: $0 random"
    exit 1
fi

MODULE_NAME=$1
MODULE_PATH="modules/${MODULE_NAME}"
JAR_FILE="$MODULE_PATH/target/mcp-${MODULE_NAME}-1.0.0-SNAPSHOT.jar"

if [ ! -d "$MODULE_PATH" ]; then
    echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
    exit 1
fi

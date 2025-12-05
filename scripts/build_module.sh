#!/bin/bash

# Build and package modules.
# Usage: ./build_module.sh [module_name]
# If no module name is provided, builds all modules.

cd "$(dirname "$0")/.." || exit 1

if [ $# -eq 0 ]; then
    echo "Building all modules..."
    mvn clean install -DskipTests
else
    MODULE_NAME=$1
    MODULE_PATH="modules/${MODULE_NAME}"
    if [ ! -d "$MODULE_PATH" ]; then
        echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
        exit 1
    fi
    echo "Building module: $MODULE_NAME"
    mvn clean install -DskipTests -pl "$MODULE_PATH" -am
fi

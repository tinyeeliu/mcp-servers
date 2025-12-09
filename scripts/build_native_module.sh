#!/bin/bash

# Build native images for modules using GraalVM.
# Usage: ./build_native.sh [module_name]
# If no module name is provided, builds native images for all modules.

cd "$(dirname "$0")/.." || exit 1

if [ $# -eq 0 ]; then
    echo "Building native images for all modules..."
    mvn clean package -DskipTests -Pnative

    # Check if build succeeded
    if [ $? -ne 0 ]; then
        echo "All modules native build failed. Exiting..."
        exit 1
    fi
else
    MODULE_NAME=$1
    MODULE_PATH="modules/${MODULE_NAME}"
    if [ ! -d "$MODULE_PATH" ]; then
        echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
        exit 1
    fi
    echo "Building native image for module: $MODULE_NAME"
    mvn clean package -DskipTests -Pnative -pl "$MODULE_PATH" -am

    # Check if build succeeded
    if [ $? -ne 0 ]; then
        echo "Module '$MODULE_NAME' native build failed. Exiting..."
        exit 1
    fi
fi
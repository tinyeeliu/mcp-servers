#!/bin/bash

# Build and package modules.
# Usage: ./build_module.sh [--clean] [module_name]
# Options:
#   --clean    - Clean target directories before building
# If no module name is provided, builds all modules.

cd "$(dirname "$0")/.." || exit 1

# Check if clean option is provided (can be anywhere in arguments)
CLEAN=""
ARGS=()
for arg in "$@"; do
    if [ "$arg" = "--clean" ]; then
        CLEAN="clean"
    else
        ARGS+=("$arg")
    fi
done

# Reset positional parameters to filtered args
set -- "${ARGS[@]}"

if [ $# -eq 0 ]; then
    echo "Building all modules..."
    if [ -n "$CLEAN" ]; then
        echo "Cleaning target directories..."
        mvn clean install -DskipTests
    else
        echo "Skipping clean (use --clean to clean)"
        mvn install -DskipTests
    fi
else
    MODULE_NAME=$1
    MODULE_PATH="modules/${MODULE_NAME}"
    if [ ! -d "$MODULE_PATH" ]; then
        echo "Error: Module '$MODULE_NAME' not found at $MODULE_PATH"
        exit 1
    fi
    echo "Building module: $MODULE_NAME"
    if [ -n "$CLEAN" ]; then
        echo "Cleaning target directories..."
        mvn clean install -DskipTests -pl "$MODULE_PATH" -am
    else
        echo "Skipping clean (use --clean to clean)"
        mvn install -DskipTests -pl "$MODULE_PATH" -am
    fi
fi
